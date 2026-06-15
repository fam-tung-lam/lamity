package com.phamtunglam.lamity.downloader

import com.phamtunglam.lamity.downloader.bridge.LamityDownloaderBridge
import com.phamtunglam.lamity.downloader.bridge.LamityDownloaderIos
import com.phamtunglam.lamity.downloader.models.DownloadProgress
import com.phamtunglam.lamity.downloader.models.DownloadRequest
import com.phamtunglam.lamity.downloader.models.DownloadState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** The platform [Downloader] for iOS, delegating to the Swift bridge. */
fun iosDownloader(): Downloader = IosDownloader(LamityDownloaderIos.requireBridge())

/**
 * Maps the Swift bridge onto the [Downloader] API. One long-lived bridge
 * observer per id feeds an in-memory progress map; flows returned by
 * [observe] just read that map, so collectors can come and go freely while
 * background downloads keep reporting.
 */
internal class IosDownloader(private val bridge: LamityDownloaderBridge) : Downloader {
    private val progressById = MutableStateFlow<Map<String, DownloadProgress?>>(emptyMap())
    private val observed = MutableStateFlow<Set<String>>(emptySet())

    override suspend fun start(request: DownloadRequest) {
        setState(request.id, DownloadState.QUEUED)
        ensureObserving(request.id)
        bridge.start(
            id = request.id,
            url = request.url,
            destinationPath = request.destinationPath,
            headerKeys = request.headers.keys.toList(),
            headerValues = request.headers.values.toList(),
            bearerToken = request.bearerToken,
            trustedAuthHosts = request.trustedAuthHosts.toList(),
            expectedSizeBytes = request.expectedSizeBytes,
            sha256 = request.sha256,
            requireUnmetered = request.requireUnmetered,
        )
    }

    override suspend fun pause(id: String) {
        bridge.pause(id)
    }

    override suspend fun resume(id: String) {
        setState(id, DownloadState.QUEUED)
        ensureObserving(id)
        bridge.resume(id)
    }

    override suspend fun cancel(id: String) {
        bridge.cancel(id)
        progressById.update { it + (id to null) }
    }

    override fun observe(id: String): Flow<DownloadProgress?> {
        ensureObserving(id)
        return progressById.map { it[id] }.distinctUntilChanged()
    }

    private fun ensureObserving(id: String) {
        if (id in observed.value) return
        observed.update { it + id }
        bridge.observe(
            id = id,
            onProgress = { state, downloadedBytes, totalBytes, bytesPerSecond, etaMillis ->
                val mapped =
                    runCatching { DownloadState.valueOf(state) }
                        .getOrDefault(DownloadState.FAILED)
                if (mapped == DownloadState.CANCELLED) {
                    progressById.update { it + (id to null) }
                } else {
                    progressById.update {
                        it + (
                            id to
                                DownloadProgress(
                                    id = id,
                                    state = mapped,
                                    downloadedBytes = downloadedBytes,
                                    totalBytes = totalBytes,
                                    bytesPerSecond = bytesPerSecond,
                                    etaMillis = etaMillis,
                                )
                        )
                    }
                }
            },
            onError = { message ->
                progressById.update { current ->
                    val previous = current[id]
                    current + (
                        id to
                            DownloadProgress(
                                id = id,
                                state = DownloadState.FAILED,
                                downloadedBytes = previous?.downloadedBytes ?: 0,
                                totalBytes = previous?.totalBytes ?: 0,
                                error = message,
                            )
                    )
                }
            },
        )
    }

    private fun setState(id: String, state: DownloadState) {
        progressById.update { current ->
            val previous = current[id]
            current + (
                id to
                    DownloadProgress(
                        id = id,
                        state = state,
                        downloadedBytes = previous?.downloadedBytes ?: 0,
                        totalBytes = previous?.totalBytes ?: 0,
                    )
            )
        }
    }
}
