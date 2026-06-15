package com.phamtunglam.lamity.feature.history.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phamtunglam.lamity.core.domain.platform.formatDateTime
import com.phamtunglam.lamity.feature.chat.data.ConversationsRepository
import com.phamtunglam.lamity.feature.chat.domain.Conversation
import com.phamtunglam.lamity.feature.history.domain.DeleteConversationUseCase
import com.phamtunglam.lamity.feature.history.domain.ObserveConversationSummariesUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ConversationRowUiState(val conversation: Conversation, val updatedAtText: String)

data class HistoryUiState(val rows: List<ConversationRowUiState> = emptyList())

class HistoryViewModel(
    observeConversationSummaries: ObserveConversationSummariesUseCase,
    private val deleteConversation: DeleteConversationUseCase,
    private val conversations: ConversationsRepository,
) : ViewModel() {
    val uiState: StateFlow<HistoryUiState> =
        observeConversationSummaries()
            .map { summaries ->
                HistoryUiState(
                    rows =
                        summaries.map { summary ->
                            ConversationRowUiState(
                                conversation = summary.conversation,
                                updatedAtText = formatDateTime(summary.conversation.updatedAt),
                            )
                        },
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun rename(conversationId: String, title: String) {
        viewModelScope.launch { conversations.rename(conversationId, title) }
    }

    fun delete(conversationId: String) {
        viewModelScope.launch { deleteConversation(conversationId) }
    }
}
