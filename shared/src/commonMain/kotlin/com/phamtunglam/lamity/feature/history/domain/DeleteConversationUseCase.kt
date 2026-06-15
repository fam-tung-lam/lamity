package com.phamtunglam.lamity.feature.history.domain

import com.phamtunglam.lamity.feature.chat.data.ConversationsRepository

/**
 * Deletes a conversation. A chat screen currently showing it resets itself by observing the
 * conversation list (see ChatViewModel), so no cross-feature call is needed here.
 */
class DeleteConversationUseCase(private val conversations: ConversationsRepository) {
    suspend operator fun invoke(conversationId: String) = conversations.delete(conversationId)
}
