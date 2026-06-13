package com.phamtunglam.lamity.feature.history.domain

import com.phamtunglam.lamity.feature.chat.data.ConversationsRepository
import com.phamtunglam.lamity.feature.chat.domain.ChatSessionManager

/** Deletes a conversation and resets the chat session if it was open. */
class DeleteConversationUseCase(
    private val conversations: ConversationsRepository,
    private val chat: ChatSessionManager,
) {
    suspend operator fun invoke(conversationId: String) {
        conversations.delete(conversationId)
        chat.onConversationDeleted(conversationId)
    }
}
