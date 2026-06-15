package com.phamtunglam.lamity.feature.models.domain

import com.phamtunglam.lamity.core.LamityBuildConfig
import com.phamtunglam.lamity.feature.models.data.ModelDownloadManager
import com.phamtunglam.lamity.feature.models.data.ModelsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** A catalog model joined with its download status and auth requirement. */
data class ModelWithStatus(
    val model: LlmModel,
    val status: ModelStatus,
    /** The model needs a HuggingFace token that the build was not configured with. */
    val needsToken: Boolean,
)

/** Streams the model catalog enriched with download status for the models list. */
class ObserveModelsWithStatusUseCase(
    private val models: ModelsRepository,
    private val downloads: ModelDownloadManager,
    /** HuggingFace token injected at build time; blank means gated models need one. */
    private val hfToken: String = LamityBuildConfig.hfToken,
) {
    operator fun invoke(): Flow<List<ModelWithStatus>> =
        combine(
            models.models,
            downloads.statuses,
        ) { modelList, statuses ->
            modelList.map { model ->
                ModelWithStatus(
                    model = model,
                    status = statuses[model.id] ?: ModelStatus.NotDownloaded,
                    needsToken = model.requiresAuth && hfToken.isBlank(),
                )
            }
        }
}
