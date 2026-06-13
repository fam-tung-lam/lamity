package com.phamtunglam.lamity.feature.studio.data

import com.phamtunglam.lamity.feature.studio.domain.Agent
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
    ): Agent

    suspend fun delete(agentId: String)

    /** Drop references to a deleted skill from every agent. */
    suspend fun detachSkillEverywhere(skillId: String)
}
