package com.phamtunglam.lamity.feature.models.data

import com.phamtunglam.lamity.downloader.models.DownloadProgress
import com.phamtunglam.lamity.downloader.models.DownloadState
import com.phamtunglam.lamity.feature.models.domain.ModelStatus

/** Folds downloader progress and on-disk presence into one [ModelStatus]. */
internal object ModelStatusMapper {

    fun map(progress: DownloadProgress?, fileExists: Boolean): ModelStatus =
        when (progress?.state) {
            null, DownloadState.CANCELLED, DownloadState.SUCCEEDED ->
                if (fileExists) ModelStatus.Downloaded else ModelStatus.NotDownloaded
            DownloadState.QUEUED -> ModelStatus.Queued
            DownloadState.RUNNING -> ModelStatus.Downloading(
                downloadedBytes = progress.downloadedBytes,
                totalBytes = progress.totalBytes,
                bytesPerSecond = progress.bytesPerSecond,
                etaMillis = progress.etaMillis,
            )
            DownloadState.PAUSED -> ModelStatus.Paused(
                downloadedBytes = progress.downloadedBytes,
                totalBytes = progress.totalBytes,
            )
            DownloadState.VERIFYING -> ModelStatus.Verifying
            DownloadState.FAILED -> ModelStatus.Failed(progress.error ?: "Download failed.")
        }
}
