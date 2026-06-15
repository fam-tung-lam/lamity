package com.phamtunglam.lamity.feature.agents.data

import com.phamtunglam.lamity.feature.agents.domain.Agent
import com.phamtunglam.lamity.feature.models.domain.ModelConfig
import kotlinx.coroutines.flow.StateFlow

interface AgentsRepository {
    val agents: StateFlow<List<Agent>>

    /** Completes once persisted agents have been loaded into [agents]. */
    suspend fun awaitLoaded()

    fun byId(id: String?): Agent?

    suspend fun upsert(
        id: String?,
        name: String,
        description: String,
        systemPrompt: String,
        toolIds: List<String>,
        skillIds: List<String>,
        modelId: String?,
        modelConfig: ModelConfig?,
    ): Agent

    suspend fun delete(agentId: String)

    /** Drop references to a deleted skill from every agent. */
    suspend fun detachSkillEverywhere(skillId: String)
}
