package com.phamtunglam.lamity.feature.studio.domain

import com.phamtunglam.lamity.feature.studio.data.AgentsRepository

/** Validates and saves an agent. Returns null when the name is blank. */
class SaveAgentUseCase(
    private val agents: AgentsRepository,
) {
    suspend operator fun invoke(
        id: String?,
        name: String,
        description: String,
        systemPrompt: String,
        toolIds: List<String>,
        skillIds: List<String>,
    ): Agent? {
        if (name.isBlank()) return null
        return agents.upsert(
            id = id,
            name = name,
            description = description,
            systemPrompt = systemPrompt,
            toolIds = toolIds,
            skillIds = skillIds,
        )
    }
}
