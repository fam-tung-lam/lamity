package com.phamtunglam.lamity.feature.models.domain

import co.touchlab.kermit.Logger
import com.phamtunglam.lamity.downloader.Downloader

/** Resumes a paused download, restarting it from scratch when nothing is stored for the model. */
class ResumeModelDownloadUseCase(private val downloader: Downloader, private val start: StartModelDownloadUseCase) {
    private val log = Logger.withTag("ResumeModelDownloadUseCase")

    suspend operator fun invoke(model: LlmModel) {
        runCatching { downloader.resume(model.id) }
            .onFailure {
                log.w { "no stored request for ${model.id}; restarting from scratch" }
                start(model)
            }
    }
}
