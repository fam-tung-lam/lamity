package com.phamtunglam.lamity.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phamtunglam.lamity.core.domain.platform.formatDateTime
import com.phamtunglam.lamity.feature.chat.data.ConversationsRepository
import com.phamtunglam.lamity.feature.chat.domain.Conversation
import com.phamtunglam.lamity.feature.chat.domain.DeleteConversationUseCase
import com.phamtunglam.lamity.feature.chat.domain.ObserveConversationsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ConversationRowUiState(val conversation: Conversation, val updatedAtText: String)

data class ChatsUiState(val rows: List<ConversationRowUiState> = emptyList())

class ChatsViewModel(
    observeConversations: ObserveConversationsUseCase,
    private val deleteConversation: DeleteConversationUseCase,
    private val conversations: ConversationsRepository,
) : ViewModel() {
    val uiState: StateFlow<ChatsUiState> =
        observeConversations()
            .map { list ->
                ChatsUiState(
                    rows =
                        list.map { conversation ->
                            ConversationRowUiState(
                                conversation = conversation,
                                updatedAtText = formatDateTime(conversation.updatedAt),
                            )
                        },
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatsUiState())

    fun rename(conversationId: String, title: String) {
        viewModelScope.launch { conversations.rename(conversationId, title) }
    }

    fun delete(conversationId: String) {
        viewModelScope.launch { deleteConversation(conversationId) }
    }
}
