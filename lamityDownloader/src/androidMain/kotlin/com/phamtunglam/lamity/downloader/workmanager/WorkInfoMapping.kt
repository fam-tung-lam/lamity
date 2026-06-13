package com.phamtunglam.lamity.downloader.workmanager

import androidx.work.WorkInfo
import com.phamtunglam.lamity.downloader.models.DownloadProgress
import com.phamtunglam.lamity.downloader.models.DownloadState
import com.phamtunglam.lamity.downloader.states.DownloadStateFlags
import com.phamtunglam.lamity.downloader.states.toDownloadState

/** Folds WorkManager state plus progress/output `Data` into a [DownloadProgress]. */
internal object WorkInfoMapping {

    fun toProgress(
        id: String,
        workInfo: WorkInfo?,
        partialBytes: Long,
        totalBytes: Long,
    ): DownloadProgress? {
        if (workInfo == null && partialBytes == 0L) return null
        val progressData = workInfo?.progress
        val outputData = workInfo?.outputData
        return DownloadProgress(
            id = id,
            state = workInfo.toState(partialBytes),
            downloadedBytes = progressData
                ?.getLong(DownloadWorkData.Keys.DOWNLOADED_BYTES, partialBytes)
                ?: partialBytes,
            totalBytes = progressData
                ?.getLong(DownloadWorkData.Keys.TOTAL_BYTES, totalBytes)
                ?: totalBytes,
            bytesPerSecond = progressData?.getLong(DownloadWorkData.Keys.BYTES_PER_SECOND, 0L) ?: 0L,
            etaMillis = progressData?.getLong(DownloadWorkData.Keys.ETA_MILLIS, 0L) ?: 0L,
            error = outputData?.getString(DownloadWorkData.Keys.ERROR),
        )
    }

    private fun WorkInfo?.toState(partialBytes: Long): DownloadState {
        // VERIFYING is reported through progress data while the work itself is RUNNING.
        val progressState = this?.progress?.getString(DownloadWorkData.Keys.STATE)
        if (this?.state == WorkInfo.State.RUNNING && progressState == DownloadState.VERIFYING.name) {
            return DownloadState.VERIFYING
        }
        return DownloadStateFlags(
            enqueued = this?.state == WorkInfo.State.ENQUEUED || this?.state == WorkInfo.State.BLOCKED,
            running = this?.state == WorkInfo.State.RUNNING,
            succeeded = this?.state == WorkInfo.State.SUCCEEDED,
            failed = this?.state == WorkInfo.State.FAILED,
            cancelled = this?.state == WorkInfo.State.CANCELLED,
            hasPartial = partialBytes > 0,
        ).toDownloadState()
    }
}
