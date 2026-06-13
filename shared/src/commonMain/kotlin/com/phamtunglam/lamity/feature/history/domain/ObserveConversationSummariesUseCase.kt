package com.phamtunglam.lamity.feature.history.domain

import com.phamtunglam.lamity.feature.chat.data.ConversationsRepository
import com.phamtunglam.lamity.feature.chat.domain.Conversation
import com.phamtunglam.lamity.feature.models.data.ModelsRepository
import com.phamtunglam.lamity.feature.studio.data.AgentsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** A conversation joined with the display names of its agent and model. */
data class ConversationSummary(
    val conversation: Conversation,
    val agentName: String?,
    val modelName: String,
)

/** Streams conversations enriched with agent and model names for the history list. */
class ObserveConversationSummariesUseCase(
    private val conversations: ConversationsRepository,
    private val agents: AgentsRepository,
    private val models: ModelsRepository,
) {
    operator fun invoke(): Flow<List<ConversationSummary>> = combine(
        conversations.conversations,
        agents.agents,
        models.models,
    ) { conversationList, agentList, modelList ->
        conversationList.map { conversation ->
            ConversationSummary(
                conversation = conversation,
                agentName = agentList.firstOrNull { it.id == conversation.agentId }?.name,
                modelName = modelList.firstOrNull { it.id == conversation.modelId }?.name
                    ?: conversation.modelId,
            )
        }
    }
}
