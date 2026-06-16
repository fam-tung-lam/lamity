package com.phamtunglam.lamity.feature.llmModels.data

import com.phamtunglam.lamity.feature.llmModels.domain.LlmModel
import com.phamtunglam.lamity.feature.llmModels.domain.ModelCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The model catalog. Every entry is built-in seed metadata defined in code ([ModelCatalog]); nothing
 * about a model is persisted. Models hold no persisted inference config — catalog defaults come from
 * the seed, and the in-use config is held in memory per chat.
 */
class ModelsRepositoryImpl : ModelsRepository {
    override val models: StateFlow<List<LlmModel>> = MutableStateFlow(ModelCatalog.seed).asStateFlow()

    override suspend fun awaitLoaded() = Unit

    override fun byId(id: String?): LlmModel? = id?.let { i -> models.value.firstOrNull { it.id == i } }
}
