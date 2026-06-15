package com.phamtunglam.lamity.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phamtunglam.lamity.feature.agents.data.AgentsRepository
import com.phamtunglam.lamity.feature.chat.data.ConversationsRepository
import com.phamtunglam.lamity.feature.models.data.ModelsRepository
import com.phamtunglam.lamity.feature.skills.data.SkillsRepository
import com.phamtunglam.lamity.feature.tools.domain.AppTool
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val agentsCount: Int = 0,
    val skillsCount: Int = 0,
    val toolsCount: Int = 0,
    val chatsCount: Int = 0,
    val modelsCount: Int = 0,
)

class HomeViewModel(
    agents: AgentsRepository,
    skills: SkillsRepository,
    conversations: ConversationsRepository,
    models: ModelsRepository,
    tools: List<AppTool>,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> =
        combine(
            agents.agents,
            skills.skills,
            conversations.conversations,
            models.models,
        ) { agentList, skillList, chatList, modelList ->
            HomeUiState(
                agentsCount = agentList.size,
                skillsCount = skillList.size,
                toolsCount = tools.size,
                chatsCount = chatList.size,
                modelsCount = modelList.size,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(toolsCount = tools.size))
}
