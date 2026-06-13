package com.phamtunglam.lamity.downloader.models

enum class DownloadState {
    /** Waiting for the platform scheduler (or a network matching the constraints). */
    QUEUED,

    /** Actively transferring bytes. */
    RUNNING,

    /** Stopped with resumable bytes retained. */
    PAUSED,

    /** Transfer finished; verifying the SHA-256 checksum. */
    VERIFYING,

    /** Finished successfully; the file is at its destination path. */
    SUCCEEDED,

    /** Finished with an error (see [DownloadProgress.error]). */
    FAILED,

    /** Stopped and disposed. */
    CANCELLED,
}
