package com.phamtunglam.lamity.feature.models.data

import com.phamtunglam.lamity.feature.models.domain.LlmModel
import kotlinx.coroutines.flow.StateFlow

interface ModelsRepository {
    val models: StateFlow<List<LlmModel>>

    /** Completes once the persisted catalog has been loaded into [models]. */
    suspend fun awaitLoaded()

    fun byId(id: String?): LlmModel?

    suspend fun addCustomModel(name: String, url: String, requiresAuth: Boolean): LlmModel

    suspend fun removeCustomModel(modelId: String)
}
