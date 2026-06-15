package com.phamtunglam.lamity.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phamtunglam.lamity.feature.agents.data.AgentsRepository
import com.phamtunglam.lamity.feature.agents.domain.Agent
import com.phamtunglam.lamity.feature.chat.domain.ChatSessionManager
import com.phamtunglam.lamity.feature.chat.domain.ChatSessionState
import com.phamtunglam.lamity.feature.models.data.ModelDownloadManager
import com.phamtunglam.lamity.feature.models.data.ModelsRepository
import com.phamtunglam.lamity.feature.models.domain.LlmModel
import com.phamtunglam.lamity.feature.skills.data.SkillsRepository
import com.phamtunglam.lamity.feature.skills.domain.Skill
import com.phamtunglam.lamity.feature.tools.domain.AppTool
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ChatUiState(
    val chat: ChatSessionState = ChatSessionState(),
    val agents: List<Agent> = emptyList(),
    val models: List<LlmModel> = emptyList(),
    val skills: List<Skill> = emptyList(),
    val selectedModelReady: Boolean = false,
)

class ChatViewModel(
    private val chat: ChatSessionManager,
    agentsRepository: AgentsRepository,
    modelsRepository: ModelsRepository,
    skillsRepository: SkillsRepository,
    downloads: ModelDownloadManager,
    val tools: List<AppTool>,
) : ViewModel() {
    val uiState: StateFlow<ChatUiState> =
        combine(
            chat.state,
            agentsRepository.agents,
            modelsRepository.models,
            skillsRepository.skills,
            downloads.statuses,
        ) { chatState, agents, models, skills, _ ->
            // The effective model is the agent's when one with a model is selected, else the chat pick.
            val agent = agents.firstOrNull { it.id == chatState.agentId }
            val effectiveModelId = agent?.modelId ?: chatState.modelId
            val selected = models.firstOrNull { it.id == effectiveModelId }
            ChatUiState(
                chat = chatState,
                agents = agents,
                models = models,
                skills = skills,
                selectedModelReady = selected != null && downloads.isDownloaded(selected),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatUiState(chat = chat.state.value))

    /** Warms the engine + native conversation for the current selection. Called when the chat opens. */
    fun prepare() = chat.prepareSession()

    fun selectAgent(agentId: String?) = chat.selectAgent(agentId)

    fun selectModel(modelId: String) = chat.selectModel(modelId)

    fun setCustomSystemPrompt(prompt: String) = chat.setCustomSystemPrompt(prompt)

    fun toggleCustomTool(toolId: String) = chat.toggleCustomTool(toolId)

    fun toggleCustomSkill(skillId: String) = chat.toggleCustomSkill(skillId)

    fun newChat() = chat.newChat()

    fun send(text: String) = chat.send(text)

    fun stopGeneration() = chat.stopGeneration()

    fun dismissError() = chat.dismissError()

    fun dismissNotice() = chat.dismissNotice()
}
