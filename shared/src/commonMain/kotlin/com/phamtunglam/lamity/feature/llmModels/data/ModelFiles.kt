package com.phamtunglam.lamity.feature.llmModels.data

import com.phamtunglam.lamity.core.domain.platform.AppDirs
import com.phamtunglam.lamity.feature.llmModels.domain.LlmModel
import com.phamtunglam.lamity.logger.LamityLogger
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

private const val TAG = "ModelFiles"

/**
 * Resolves the on-disk location of downloaded model files and answers presence checks. Stateless and
 * scope-free (it only ensures [AppDirs.modelsDir] exists), so it is safe to share app-wide: both the
 * chat engine loader and the models screen depend on it independently of any screen lifecycle.
 */
class ModelFiles(private val dirs: AppDirs, private val fileSystem: FileSystem) {
    init {
        runCatching { fileSystem.createDirectories(dirs.modelsDir.toPath()) }
            .onFailure { LamityLogger.w(TAG, it) { "Failed to create models directory ${dirs.modelsDir}" } }
    }

    private fun modelFile(model: LlmModel): Path = dirs.modelsDir.toPath() / model.fileName

    fun modelPath(model: LlmModel): String = modelFile(model).toString()

    fun isDownloaded(model: LlmModel): Boolean = fileSystem.exists(modelFile(model))

    /** Deletes the downloaded file if present. */
    fun delete(model: LlmModel) {
        runCatching { fileSystem.delete(modelFile(model)) }
            .onFailure { LamityLogger.w(TAG, it) { "Failed to delete ${model.id}" } }
    }
}
