package com.phamtunglam.lamity.feature.models.domain

import com.phamtunglam.lamity.feature.models.data.ModelDownloadManager
import com.phamtunglam.lamity.feature.models.data.ModelsRepository

/** Removes a custom model: stops any download, deletes the file, drops the catalog entry. */
class RemoveCustomModelUseCase(
    private val models: ModelsRepository,
    private val downloads: ModelDownloadManager,
) {
    suspend operator fun invoke(model: LlmModel) {
        downloads.cancel(model)
        downloads.deleteFile(model)
        models.removeCustomModel(model.id)
    }
}
