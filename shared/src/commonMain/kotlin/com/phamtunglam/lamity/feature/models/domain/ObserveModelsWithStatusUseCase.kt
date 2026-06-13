package com.phamtunglam.lamity.feature.models.domain

import com.phamtunglam.lamity.feature.models.data.ModelDownloadManager
import com.phamtunglam.lamity.feature.models.data.ModelsRepository
import com.phamtunglam.lamity.feature.settings.data.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** A catalog model joined with its download status and auth requirement. */
data class ModelWithStatus(
    val model: LlmModel,
    val status: ModelStatus,
    /** The model needs a HuggingFace token that the user has not configured yet. */
    val needsToken: Boolean,
)

/** Streams the model catalog enriched with download status for the models list. */
class ObserveModelsWithStatusUseCase(
    private val models: ModelsRepository,
    private val downloads: ModelDownloadManager,
    private val settings: SettingsRepository,
) {
    operator fun invoke(): Flow<List<ModelWithStatus>> = combine(
        models.models,
        downloads.statuses,
        settings.settings,
    ) { modelList, statuses, appSettings ->
        modelList.map { model ->
            ModelWithStatus(
                model = model,
                status = statuses[model.id] ?: ModelStatus.NotDownloaded,
                needsToken = model.requiresAuth && appSettings.hfToken.isBlank(),
            )
        }
    }
}
