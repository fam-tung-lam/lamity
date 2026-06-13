package com.phamtunglam.lamity.downloader.models

/**
 * Point-in-time snapshot of one download. [totalBytes] is 0 when unknown;
 * [bytesPerSecond] and [etaMillis] are 0 until enough samples exist.
 */
data class DownloadProgress(
    val id: String,
    val state: DownloadState,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val bytesPerSecond: Long = 0,
    val etaMillis: Long = 0,
    val error: String? = null,
)
