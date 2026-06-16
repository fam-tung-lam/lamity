package com.phamtunglam.lamity.feature.llmModels.domain

import com.phamtunglam.lamity.downloader.Downloader

/** Pauses the background download of [model], keeping partial bytes for a later resume. */
class PauseModelDownloadUseCase(private val downloader: Downloader) {
    suspend operator fun invoke(model: LlmModel) = downloader.pause(model.id)
}
