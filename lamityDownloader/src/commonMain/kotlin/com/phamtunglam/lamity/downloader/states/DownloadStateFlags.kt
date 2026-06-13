package com.phamtunglam.lamity.downloader.states

import com.phamtunglam.lamity.downloader.models.DownloadState

/**
 * Platform scheduler facts about one download, folded into a single
 * [DownloadState]. A cancelled-or-finished work item that left partial bytes
 * behind is reported as [DownloadState.PAUSED] so the UI can offer resume.
 */
internal data class DownloadStateFlags(
    val enqueued: Boolean = false,
    val running: Boolean = false,
    val succeeded: Boolean = false,
    val failed: Boolean = false,
    val cancelled: Boolean = false,
    val hasPartial: Boolean = false,
)

internal fun DownloadStateFlags.toDownloadState(): DownloadState =
    when {
        succeeded -> DownloadState.SUCCEEDED
        failed -> DownloadState.FAILED
        running -> DownloadState.RUNNING
        enqueued -> DownloadState.QUEUED
        cancelled && hasPartial -> DownloadState.PAUSED
        cancelled -> DownloadState.CANCELLED
        hasPartial -> DownloadState.PAUSED
        else -> DownloadState.QUEUED
    }
