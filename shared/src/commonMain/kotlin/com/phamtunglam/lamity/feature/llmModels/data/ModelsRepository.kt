package com.phamtunglam.lamity.feature.llmModels.data

import com.phamtunglam.lamity.feature.llmModels.domain.LlmModel
import kotlinx.coroutines.flow.StateFlow

interface ModelsRepository {
    val models: StateFlow<List<LlmModel>>

    /** Completes once the catalog has been loaded into [models]. */
    suspend fun awaitLoaded()

    fun byId(id: String?): LlmModel?
}
