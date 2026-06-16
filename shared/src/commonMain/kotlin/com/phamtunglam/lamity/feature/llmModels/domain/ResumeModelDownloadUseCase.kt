package com.phamtunglam.lamity.feature.llmModels.domain

import com.phamtunglam.lamity.downloader.Downloader
import com.phamtunglam.lamity.logger.LamityLogger

private const val TAG = "ResumeModelDownloadUseCase"

/** Resumes a paused download, restarting it from scratch when nothing is stored for the model. */
class ResumeModelDownloadUseCase(private val downloader: Downloader, private val start: StartModelDownloadUseCase) {
    suspend operator fun invoke(model: LlmModel) {
        runCatching { downloader.resume(model.id) }
            .onFailure {
                LamityLogger.w(TAG) { "no stored request for ${model.id}; restarting from scratch" }
                start(model)
            }
    }
}
