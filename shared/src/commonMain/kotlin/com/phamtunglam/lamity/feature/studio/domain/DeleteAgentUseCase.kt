package com.phamtunglam.lamity.feature.studio.domain

import com.phamtunglam.lamity.feature.chat.domain.ChatSessionManager
import com.phamtunglam.lamity.feature.studio.data.AgentsRepository

/** Deletes an agent and deselects it in the chat session if it was active. */
class DeleteAgentUseCase(private val agents: AgentsRepository, private val chat: ChatSessionManager) {
    suspend operator fun invoke(agentId: String) {
        agents.delete(agentId)
        if (chat.state.value.agentId == agentId) chat.selectAgent(null)
    }
}
