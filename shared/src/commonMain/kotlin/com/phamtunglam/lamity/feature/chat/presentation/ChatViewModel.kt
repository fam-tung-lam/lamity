package com.phamtunglam.lamity.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phamtunglam.lamity.feature.chat.domain.ChatSessionManager
import com.phamtunglam.lamity.feature.chat.domain.ChatSessionState
import com.phamtunglam.lamity.feature.models.data.ModelDownloadManager
import com.phamtunglam.lamity.feature.models.data.ModelsRepository
import com.phamtunglam.lamity.feature.models.domain.LlmModel
import com.phamtunglam.lamity.feature.studio.data.AgentsRepository
import com.phamtunglam.lamity.feature.studio.domain.Agent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ChatUiState(
    val chat: ChatSessionState = ChatSessionState(),
    val agents: List<Agent> = emptyList(),
    val models: List<LlmModel> = emptyList(),
    val selectedModelReady: Boolean = false,
)

class ChatViewModel(
    private val chat: ChatSessionManager,
    agentsRepository: AgentsRepository,
    modelsRepository: ModelsRepository,
    downloads: ModelDownloadManager,
) : ViewModel() {
    val uiState: StateFlow<ChatUiState> =
        combine(
            chat.state,
            agentsRepository.agents,
            modelsRepository.models,
            downloads.statuses,
        ) { chatState, agents, models, _ ->
            val selected = models.firstOrNull { it.id == chatState.modelId }
            ChatUiState(
                chat = chatState,
                agents = agents,
                models = models,
                selectedModelReady = selected != null && downloads.isDownloaded(selected),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatUiState(chat = chat.state.value))

    fun selectAgent(agentId: String?) = chat.selectAgent(agentId)

    fun selectModel(modelId: String) = chat.selectModel(modelId)

    fun newChat() = chat.newChat()

    fun send(text: String) = chat.send(text)

    fun stopGeneration() = chat.stopGeneration()

    fun dismissError() = chat.dismissError()
}
