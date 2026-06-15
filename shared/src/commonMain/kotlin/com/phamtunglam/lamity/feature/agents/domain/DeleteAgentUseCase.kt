package com.phamtunglam.lamity.feature.agents.domain

import com.phamtunglam.lamity.feature.agents.data.AgentsRepository
import com.phamtunglam.lamity.feature.settings.data.SettingsRepository

/** Deletes an agent and clears it from the persisted chat selection if it was the active one. */
class DeleteAgentUseCase(private val agents: AgentsRepository, private val settings: SettingsRepository) {
    suspend operator fun invoke(agentId: String) {
        agents.delete(agentId)
        if (settings.value.lastAgentId == agentId) {
            settings.setLastSelection(settings.value.lastModelId, null)
        }
    }
}
