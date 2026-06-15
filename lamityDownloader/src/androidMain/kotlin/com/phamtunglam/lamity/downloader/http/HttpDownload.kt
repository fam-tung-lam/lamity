package com.phamtunglam.lamity.downloader.http

import com.phamtunglam.lamity.downloader.models.DownloadException
import com.phamtunglam.lamity.downloader.models.DownloadRequest
import com.phamtunglam.lamity.downloader.models.isAuthTrustedHost
import com.phamtunglam.lamity.downloader.utils.DownloadRate
import com.phamtunglam.lamity.downloader.workmanager.partialFile
import okio.FileSystem
import okio.Path
import okio.buffer
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

internal class PreparedConnection(
    val connection: HttpURLConnection,
    /** Append to the partial file (server honored the Range request). */
    val append: Boolean,
    /** Bytes kept from the previous attempt. */
    val retainedBytes: Long,
)

/**
 * Resumable HTTP transfer into `<destination>.part`. Redirects are followed
 * manually so the Authorization header only ever reaches trusted hosts; a
 * partial file from an earlier attempt is continued with an HTTP Range
 * request when the server supports it.
 */
internal object HttpDownload {
    private val fileSystem = FileSystem.SYSTEM

    suspend fun download(
        request: DownloadRequest,
        isStopped: () -> Boolean,
        onProgress: suspend (downloadedBytes: Long, totalBytes: Long, bytesPerSecond: Long, etaMillis: Long) -> Unit,
    ): Path {
        val partialFile = request.partialFile()
        partialFile.parent?.let { fileSystem.createDirectories(it) }
        if (request.expectedSizeBytes > 0 && partialFile.sizeBytes() == request.expectedSizeBytes) {
            return partialFile
        }

        val prepared = openFollowingRedirects(request, partialFile)
        try {
            val totalBytes =
                progressTotalBytes(
                    connection = prepared.connection,
                    retainedBytes = prepared.retainedBytes,
                    totalBytesHint = request.expectedSizeBytes,
                )
            transfer(partialFile, prepared, totalBytes, isStopped, onProgress)
        } finally {
            runCatching { prepared.connection.disconnect() }
        }
        return partialFile
    }

    private fun openFollowingRedirects(request: DownloadRequest, partialFile: Path): PreparedConnection {
        var currentUrl = URL(request.url)
        val partialBytes = partialFile.sizeBytes()
        val rangeRequested = partialBytes > 0
        repeat(MAX_REDIRECTS) {
            val connection =
                (currentUrl.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = CONNECT_TIMEOUT_MILLIS
                    readTimeout = READ_TIMEOUT_MILLIS
                    request.headers.forEach { (name, value) -> setRequestProperty(name, value) }
                    if (rangeRequested) {
                        setRequestProperty("Range", "bytes=$partialBytes-")
                        // Transparent compression would corrupt byte-offset resumes.
                        setRequestProperty("Accept-Encoding", "identity")
                    }
                    if (request.bearerToken != null && request.isAuthTrustedHost(currentUrl.host)) {
                        setRequestProperty("Authorization", "Bearer ${request.bearerToken}")
                    }
                }
            val code = connection.responseCode
            when {
                code in REDIRECT_CODES -> {
                    val location =
                        connection.getHeaderField("Location")
                            ?: throw IOException("HTTP $code without Location header")
                    connection.disconnect()
                    currentUrl = URL(currentUrl, location)
                }

                code == HttpURLConnection.HTTP_OK || code == HttpURLConnection.HTTP_PARTIAL -> {
                    val append = code == HttpURLConnection.HTTP_PARTIAL && rangeRequested
                    if (!append && partialBytes > 0) fileSystem.delete(partialFile, mustExist = false)
                    return PreparedConnection(
                        connection = connection,
                        append = append,
                        retainedBytes = if (append) partialBytes else 0L,
                    )
                }

                else -> {
                    connection.disconnect()
                    failHttp(code)
                }
            }
        }
        throw IOException("Too many redirects")
    }

    private fun failHttp(code: Int): Nothing {
        val hint =
            if (code == HttpURLConnection.HTTP_UNAUTHORIZED || code == HttpURLConnection.HTTP_FORBIDDEN) {
                " — the resource may require a valid access token"
            } else {
                ""
            }
        throw IOException("HTTP $code$hint")
    }

    private suspend fun transfer(
        partialFile: Path,
        prepared: PreparedConnection,
        totalBytes: Long,
        isStopped: () -> Boolean,
        onProgress: suspend (downloadedBytes: Long, totalBytes: Long, bytesPerSecond: Long, etaMillis: Long) -> Unit,
    ) {
        val rate = DownloadRate()
        var downloadedBytes = prepared.retainedBytes
        var bytesSinceLastProgress = 0L
        var lastProgressMillis = 0L

        prepared.connection.inputStream.use { input ->
            val rawSink =
                if (prepared.append) {
                    fileSystem.appendingSink(partialFile)
                } else {
                    fileSystem.sink(partialFile)
                }
            rawSink.buffer().use { output ->
                val buffer = ByteArray(BUFFER_SIZE)
                while (true) {
                    if (isStopped()) throw DownloadException("Download stopped.")
                    val bytesRead = input.read(buffer)
                    if (bytesRead == -1) break
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    bytesSinceLastProgress += bytesRead

                    val nowMillis = System.currentTimeMillis()
                    if (nowMillis - lastProgressMillis > PROGRESS_INTERVAL_MILLIS) {
                        val snapshot =
                            rate.record(
                                bytesRead = bytesSinceLastProgress,
                                downloadedBytes = downloadedBytes,
                                totalBytes = totalBytes,
                                nowMillis = nowMillis,
                            )
                        bytesSinceLastProgress = 0L
                        lastProgressMillis = nowMillis
                        onProgress(downloadedBytes, totalBytes, snapshot.bytesPerSecond, snapshot.etaMillis)
                    }
                }
            }
        }
    }

    private fun progressTotalBytes(connection: HttpURLConnection, retainedBytes: Long, totalBytesHint: Long): Long {
        if (totalBytesHint > 0) return totalBytesHint
        val contentRangeTotal =
            connection
                .getHeaderField("Content-Range")
                ?.substringAfterLast('/')
                ?.toLongOrNull()
        if (contentRangeTotal != null && contentRangeTotal > 0) return contentRangeTotal
        val contentLength = connection.contentLengthLong
        return if (contentLength > 0) retainedBytes + contentLength else 0
    }

    /** On-disk size in bytes, or 0 when the file does not exist yet. */
    private fun Path.sizeBytes(): Long = fileSystem.metadataOrNull(this)?.size ?: 0L

    private val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
    private const val MAX_REDIRECTS = 8
    private const val BUFFER_SIZE = 256 * 1024
    private const val CONNECT_TIMEOUT_MILLIS = 30_000
    private const val READ_TIMEOUT_MILLIS = 60_000
    private const val PROGRESS_INTERVAL_MILLIS = 200L
}
