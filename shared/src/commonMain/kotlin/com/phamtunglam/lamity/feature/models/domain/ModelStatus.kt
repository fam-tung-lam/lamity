package com.phamtunglam.lamity.feature.models.domain

/** Lifecycle of a model file on this device, as shown in the catalog UI. */
sealed interface ModelStatus {
    data object NotDownloaded : ModelStatus

    /** Waiting for the scheduler or for a network matching the constraints. */
    data object Queued : ModelStatus

    data class Downloading(
        val downloadedBytes: Long,
        val totalBytes: Long,
        val bytesPerSecond: Long,
        val etaMillis: Long,
    ) : ModelStatus

    data class Paused(val downloadedBytes: Long, val totalBytes: Long) : ModelStatus

    /** Transfer done; checking the file's integrity. */
    data object Verifying : ModelStatus

    data object Downloaded : ModelStatus

    data class Failed(val message: String) : ModelStatus
}
