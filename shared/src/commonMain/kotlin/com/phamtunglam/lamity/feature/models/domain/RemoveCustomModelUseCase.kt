package com.phamtunglam.lamity.feature.models.domain

import com.phamtunglam.lamity.feature.models.data.ModelsRepository

/** Removes a custom model: stops any download, deletes the file, drops the catalog entry. */
class RemoveCustomModelUseCase(
    private val models: ModelsRepository,
    private val cancelDownload: CancelModelDownloadUseCase,
    private val deleteModelFile: DeleteModelFileUseCase,
) {
    suspend operator fun invoke(model: LlmModel) {
        cancelDownload(model)
        deleteModelFile(model)
        models.removeCustomModel(model.id)
    }
}
