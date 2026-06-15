package com.phamtunglam.lamity.feature.models.data

import co.touchlab.kermit.Logger
import com.phamtunglam.lamity.core.domain.platform.AppDirs
import com.phamtunglam.lamity.feature.models.domain.LlmModel
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/**
 * Resolves the on-disk location of downloaded model files and answers presence checks. Stateless and
 * scope-free (it only ensures [AppDirs.modelsDir] exists), so it is safe to share app-wide: both the
 * chat engine loader and the models screen depend on it independently of any screen lifecycle.
 */
class ModelFiles(private val dirs: AppDirs, private val fileSystem: FileSystem) {
    private val log = Logger.withTag("ModelFiles")

    init {
        runCatching { fileSystem.createDirectories(dirs.modelsDir.toPath()) }
            .onFailure { log.w(it) { "Failed to create models directory ${dirs.modelsDir}" } }
    }

    private fun modelFile(model: LlmModel): Path = dirs.modelsDir.toPath() / model.fileName

    fun modelPath(model: LlmModel): String = modelFile(model).toString()

    fun isDownloaded(model: LlmModel): Boolean = fileSystem.exists(modelFile(model))

    /** Deletes the downloaded file if present. */
    fun delete(model: LlmModel) {
        runCatching { fileSystem.delete(modelFile(model)) }
            .onFailure { log.w(it) { "Failed to delete ${model.id}" } }
    }
}
