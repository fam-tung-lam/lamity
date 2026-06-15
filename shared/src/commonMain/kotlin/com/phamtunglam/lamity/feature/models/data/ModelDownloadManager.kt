package com.phamtunglam.lamity.feature.models.data

import co.touchlab.kermit.Logger
import com.phamtunglam.lamity.core.LamityBuildConfig
import com.phamtunglam.lamity.core.domain.platform.AppDirs
import com.phamtunglam.lamity.downloader.Downloader
import com.phamtunglam.lamity.downloader.models.DownloadRequest
import com.phamtunglam.lamity.feature.models.data.ModelsRepository
import com.phamtunglam.lamity.feature.models.domain.LlmModel
import com.phamtunglam.lamity.feature.models.domain.ModelStatus
import com.phamtunglam.lamity.feature.settings.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/**
 * Connects the model catalog to the background [Downloader]: one download per
 * model id, surfaced as a [ModelStatus] map the UI renders directly. Files
 * land in [AppDirs.modelsDir] under the model's file name.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ModelDownloadManager(
    private val dirs: AppDirs,
    private val fileSystem: FileSystem,
    private val settings: SettingsRepository,
    private val downloader: Downloader,
    models: ModelsRepository,
    private val scope: CoroutineScope,
    /** HuggingFace token injected at build time; blank disables authenticated downloads. */
    private val hfToken: String = LamityBuildConfig.hfToken,
) {
    private val log = Logger.withTag("ModelDownloadManager")

    /** Bumped after local file changes (delete) to re-check on-disk state. */
    private val filesChanged = MutableStateFlow(0)

    val statuses: StateFlow<Map<String, ModelStatus>> =
        combine(models.models, filesChanged) { list, _ -> list }
            .flatMapLatest { list -> statusesOf(list) }
            .stateIn(scope, SharingStarted.Eagerly, emptyMap())

    init {
        runCatching { fileSystem.createDirectories(dirs.modelsDir.toPath()) }
            .onFailure { log.w(it) { "Failed to create models directory ${dirs.modelsDir}" } }
    }

    private fun modelFile(model: LlmModel): Path = dirs.modelsDir.toPath() / model.fileName

    fun modelPath(model: LlmModel): String = modelFile(model).toString()

    fun isDownloaded(model: LlmModel): Boolean = fileSystem.exists(modelFile(model))

    fun statusOf(model: LlmModel): ModelStatus =
        statuses.value[model.id]
            ?: if (isDownloaded(model)) ModelStatus.Downloaded else ModelStatus.NotDownloaded

    fun start(model: LlmModel) {
        log.i { "starting download of ${model.id}" }
        scope.launch { downloader.start(model.toDownloadRequest()) }
    }

    fun pause(model: LlmModel) {
        scope.launch { downloader.pause(model.id) }
    }

    fun resume(model: LlmModel) {
        scope.launch {
            runCatching { downloader.resume(model.id) }
                .onFailure {
                    log.w { "no stored request for ${model.id}; restarting from scratch" }
                    downloader.start(model.toDownloadRequest())
                }
        }
    }

    fun cancel(model: LlmModel) {
        scope.launch { downloader.cancel(model.id) }
    }

    /** Clears a failed download so the row returns to its idle state. */
    fun dismissError(model: LlmModel) = cancel(model)

    fun deleteFile(model: LlmModel) {
        runCatching { fileSystem.delete(modelFile(model)) }
            .onFailure { log.w(it) { "Failed to delete ${model.id}" } }
        filesChanged.update { it + 1 }
    }

    private fun statusesOf(models: List<LlmModel>) =
        if (models.isEmpty()) {
            flowOf(emptyMap())
        } else {
            combine(
                models.map { model ->
                    downloader
                        .observe(model.id)
                        .onStart { emit(null) }
                        .map { progress -> model.id to ModelStatusMapper.map(progress, isDownloaded(model)) }
                },
            ) { entries -> entries.toMap() }
        }

    private fun LlmModel.toDownloadRequest(): DownloadRequest {
        val appSettings = settings.value
        return DownloadRequest(
            id = id,
            url = url,
            destinationPath = modelPath(this),
            displayName = name,
            bearerToken =
                hfToken.takeIf {
                    it.isNotBlank() && url.contains("huggingface.co")
                },
            trustedAuthHosts = setOf("huggingface.co"),
            expectedSizeBytes = sizeBytes,
            requireUnmetered = appSettings.wifiOnlyDownloads,
        )
    }
}
