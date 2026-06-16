package com.phamtunglam.lamity.feature.llmModels.domain

import com.phamtunglam.lamity.feature.llmModels.data.ModelFiles

/** Deletes the downloaded file for [model] from disk. */
class DeleteModelFileUseCase(private val modelFiles: ModelFiles) {
    operator fun invoke(model: LlmModel) = modelFiles.delete(model)
}
