package com.phamtunglam.lamity.feature.chat.domain

import com.phamtunglam.lamity.feature.chat.data.ConversationsRepository

/**
 * Deletes a conversation. A chat screen currently showing it resets itself by observing the
 * conversation list (see ChatViewModel).
 */
class DeleteConversationUseCase(private val conversations: ConversationsRepository) {
    suspend operator fun invoke(conversationId: String) = conversations.delete(conversationId)
}
