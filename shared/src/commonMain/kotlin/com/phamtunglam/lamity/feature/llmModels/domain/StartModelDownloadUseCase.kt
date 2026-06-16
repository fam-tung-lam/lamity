package com.phamtunglam.lamity.feature.llmModels.domain

import com.phamtunglam.lamity.core.LamityBuildConfig
import com.phamtunglam.lamity.downloader.Downloader
import com.phamtunglam.lamity.downloader.models.DownloadRequest
import com.phamtunglam.lamity.feature.llmModels.data.ModelFiles
import com.phamtunglam.lamity.feature.settings.data.SettingsRepository

/** Starts (or restarts) the background download of [model], building its request from app settings. */
class StartModelDownloadUseCase(
    private val downloader: Downloader,
    private val settings: SettingsRepository,
    private val modelFiles: ModelFiles,
    /** HuggingFace token injected at build time; blank disables authenticated downloads. */
    private val hfToken: String = LamityBuildConfig.hfToken,
) {
    suspend operator fun invoke(model: LlmModel) = downloader.start(model.toDownloadRequest())

    private fun LlmModel.toDownloadRequest(): DownloadRequest =
        DownloadRequest(
            id = id,
            url = url,
            destinationPath = modelFiles.modelPath(this),
            displayName = name,
            bearerToken = hfToken.takeIf { it.isNotBlank() && url.contains("huggingface.co") },
            trustedAuthHosts = setOf("huggingface.co"),
            expectedSizeBytes = sizeBytes,
            requireUnmetered = settings.value.wifiOnlyDownloads,
        )
}
