package com.phamtunglam.lamity.feature.chat.domain

import com.phamtunglam.lamity.feature.chat.data.ConversationsRepository
import kotlinx.coroutines.flow.Flow

/**
 * Streams conversations for the chat list (home screen). Conversations are decoupled from agents and
 * models, so each row is just the conversation itself (title + timestamps).
 */
class ObserveConversationsUseCase(private val conversations: ConversationsRepository) {
    operator fun invoke(): Flow<List<Conversation>> = conversations.conversations
}
