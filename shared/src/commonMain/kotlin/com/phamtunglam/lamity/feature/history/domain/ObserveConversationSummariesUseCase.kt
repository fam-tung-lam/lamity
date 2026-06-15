package com.phamtunglam.lamity.feature.history.domain

import com.phamtunglam.lamity.feature.chat.data.ConversationsRepository
import com.phamtunglam.lamity.feature.chat.domain.Conversation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** A conversation prepared for the history list. */
data class ConversationSummary(val conversation: Conversation)

/**
 * Streams conversations for the history list. Conversations are decoupled from agents and models, so
 * the summary is just the conversation itself (title + timestamps).
 */
class ObserveConversationSummariesUseCase(private val conversations: ConversationsRepository) {
    operator fun invoke(): Flow<List<ConversationSummary>> =
        conversations.conversations.map { list -> list.map { ConversationSummary(it) } }
}
