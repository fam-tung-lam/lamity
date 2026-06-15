package com.phamtunglam.lamity.feature.models.domain

import com.phamtunglam.lamity.downloader.Downloader

/** Cancels the background download of [model], disposing partial bytes and stored request state. */
class CancelModelDownloadUseCase(private val downloader: Downloader) {
    suspend operator fun invoke(model: LlmModel) = downloader.cancel(model.id)
}
