package com.phamtunglam.lamity.feature.agents.domain

import com.phamtunglam.lamity.feature.agents.data.AgentsRepository
import com.phamtunglam.lamity.feature.models.domain.ModelConfig

/** Validates and saves an agent. Returns null when the name is blank. */
class SaveAgentUseCase(private val agents: AgentsRepository) {
    @Suppress("LongParameterList") // Mirrors the agent's editable fields.
    suspend operator fun invoke(
        id: String?,
        name: String,
        description: String,
        systemPrompt: String,
        toolIds: List<String>,
        skillIds: List<String>,
        modelId: String?,
        modelConfig: ModelConfig?,
    ): Agent? {
        if (name.isBlank()) return null
        return agents.upsert(
            id = id,
            name = name,
            description = description,
            systemPrompt = systemPrompt,
            toolIds = toolIds,
            skillIds = skillIds,
            modelId = modelId,
            modelConfig = modelConfig,
        )
    }
}
