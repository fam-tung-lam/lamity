package com.phamtunglam.lamity.downloader.workmanager

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import co.touchlab.kermit.Logger
import com.phamtunglam.lamity.downloader.checksums.Sha256
import com.phamtunglam.lamity.downloader.http.HttpDownload
import com.phamtunglam.lamity.downloader.models.DownloadException
import com.phamtunglam.lamity.downloader.models.DownloadRequest
import com.phamtunglam.lamity.downloader.models.DownloadState
import com.phamtunglam.lamity.downloader.notifications.DownloadNotification
import com.phamtunglam.lamity.downloader.persistence.RequestStore
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Downloads one file: transfer (resuming any partial bytes), optional SHA-256
 * verification, then an atomic move to the destination path. Progress goes to
 * WorkManager progress data and a foreground notification.
 */
internal class DownloadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    private val log = Logger.withTag("DownloadWorker")
    private val store = RequestStore(applicationContext)

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val request = inputData.getString(DownloadWorkData.Keys.ID)?.let(store::load)
        return notification(request).foregroundInfo(request?.displayName ?: "Download", 0)
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val id = inputData.getString(DownloadWorkData.Keys.ID)
            ?: return@withContext Result.failure(errorData("Missing download id."))
        val request = store.load(id)
            ?: return@withContext Result.failure(errorData("No stored download request for '$id'."))

        val notification = notification(request)
        runCatching { setForeground(notification.foregroundInfo(request.displayName, 0)) }

        try {
            var lastPercent = -1
            val partialFile = HttpDownload.download(
                request = request,
                isStopped = { isStopped },
                onProgress = { downloadedBytes, totalBytes, bytesPerSecond, etaMillis ->
                    publishProgress(
                        request = request,
                        state = DownloadState.RUNNING,
                        downloadedBytes = downloadedBytes,
                        totalBytes = totalBytes,
                        bytesPerSecond = bytesPerSecond,
                        etaMillis = etaMillis,
                    )
                    val percent = percentOf(downloadedBytes, totalBytes)
                    if (percent != lastPercent) {
                        lastPercent = percent
                        runCatching { setForeground(notification.foregroundInfo(request.displayName, percent)) }
                    }
                },
            )
            verify(request, partialFile)
            moveToDestination(partialFile, request.destinationPath)
            store.delete(request.id)
            log.i { "download ${request.id} complete (${File(request.destinationPath).length()} bytes)" }
            Result.success(
                workDataOf(DownloadWorkData.Keys.DOWNLOADED_BYTES to File(request.destinationPath).length()),
            )
        } catch (e: DownloadException) {
            failure(request, e)
        } catch (e: IOException) {
            failure(request, e)
        }
    }

    private suspend fun verify(request: DownloadRequest, partialFile: File) {
        if (request.expectedSizeBytes > 0 && partialFile.length() != request.expectedSizeBytes) {
            // Catalog sizes are advisory; mismatches are logged, not fatal.
            log.w {
                "size mismatch for ${request.id}: expected ${request.expectedSizeBytes}, " +
                    "got ${partialFile.length()}"
            }
        }
        val expectedSha256 = request.sha256 ?: return
        publishProgress(
            request = request,
            state = DownloadState.VERIFYING,
            downloadedBytes = partialFile.length(),
            totalBytes = partialFile.length(),
        )
        val actualSha256 = Sha256.of(partialFile)
        if (!actualSha256.equals(expectedSha256, ignoreCase = true)) {
            partialFile.delete()
            throw DownloadException("Checksum mismatch — the downloaded file is corrupt.")
        }
    }

    private fun moveToDestination(partialFile: File, destinationPath: String) {
        val destination = File(destinationPath)
        destination.delete()
        if (!partialFile.renameTo(destination)) {
            throw DownloadException("Could not move downloaded file into place.")
        }
    }

    private suspend fun publishProgress(
        request: DownloadRequest,
        state: DownloadState,
        downloadedBytes: Long,
        totalBytes: Long,
        bytesPerSecond: Long = 0,
        etaMillis: Long = 0,
    ) {
        setProgress(
            workDataOf(
                DownloadWorkData.Keys.STATE to state.name,
                DownloadWorkData.Keys.DOWNLOADED_BYTES to downloadedBytes,
                DownloadWorkData.Keys.TOTAL_BYTES to totalBytes,
                DownloadWorkData.Keys.BYTES_PER_SECOND to bytesPerSecond,
                DownloadWorkData.Keys.ETA_MILLIS to etaMillis,
            ),
        )
    }

    private fun failure(request: DownloadRequest, e: Exception): Result {
        log.w(e) { "download ${request.id} failed" }
        return Result.failure(errorData(e.message ?: "Download failed."))
    }

    private fun errorData(message: String) = workDataOf(DownloadWorkData.Keys.ERROR to message)

    private fun notification(request: DownloadRequest?) = DownloadNotification(
        applicationContext,
        notificationId = NOTIFICATION_ID_BASE + (request?.id?.hashCode() ?: 0),
    )

    private fun percentOf(downloadedBytes: Long, totalBytes: Long): Int =
        if (totalBytes > 0) (downloadedBytes * 100 / totalBytes).toInt() else 0

    private companion object {
        const val NOTIFICATION_ID_BASE = 0x4C414D
    }
}
