package com.phamtunglam.lamity.downloader

import com.phamtunglam.lamity.downloader.models.DownloadProgress
import com.phamtunglam.lamity.downloader.models.DownloadRequest
import kotlinx.coroutines.flow.Flow

/**
 * Background-capable file downloader. Downloads survive process death
 * (WorkManager on Android, background URLSession on iOS), can be paused and
 * resumed, and are verified against an optional SHA-256 checksum.
 *
 * Implementations: [AndroidDownloader] (androidMain) and `IosDownloader`
 * (iosMain, backed by a Swift bridge over a background URLSession).
 */
interface Downloader {

    /** Starts (or restarts) the download for [request], replacing any active one with the same id. */
    suspend fun start(request: DownloadRequest)

    /** Stops the transfer but keeps partial bytes so [resume] can continue it. */
    suspend fun pause(id: String)

    /** Continues a paused download. Throws [models.DownloadException] when nothing is stored for [id]. */
    suspend fun resume(id: String)

    /** Stops the transfer and disposes partial bytes and stored request state. */
    suspend fun cancel(id: String)

    /**
     * Progress of the download with [id]; emits `null` while nothing is known
     * about it (never started, or cancelled and cleaned up).
     */
    fun observe(id: String): Flow<DownloadProgress?>
}
