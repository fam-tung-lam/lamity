package com.phamtunglam.lamity.feature.studio.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phamtunglam.lamity.core.domain.tools.AppTool
import com.phamtunglam.lamity.feature.settings.data.SettingsRepository
import com.phamtunglam.lamity.feature.studio.data.AgentsRepository
import com.phamtunglam.lamity.feature.studio.data.SkillsRepository
import com.phamtunglam.lamity.feature.studio.domain.Agent
import com.phamtunglam.lamity.feature.studio.domain.DeleteAgentUseCase
import com.phamtunglam.lamity.feature.studio.domain.DeleteSkillUseCase
import com.phamtunglam.lamity.feature.studio.domain.Skill
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StudioUiState(
    val agents: List<Agent> = emptyList(),
    val skills: List<Skill> = emptyList(),
    val tools: List<AppTool> = emptyList(),
    val toolEnabled: Map<String, Boolean> = emptyMap(),
)

class StudioViewModel(
    agents: AgentsRepository,
    private val skills: SkillsRepository,
    private val settings: SettingsRepository,
    tools: List<AppTool>,
    private val deleteAgentUseCase: DeleteAgentUseCase,
    private val deleteSkillUseCase: DeleteSkillUseCase,
) : ViewModel() {
    val uiState: StateFlow<StudioUiState> =
        combine(
            agents.agents,
            skills.skills,
            settings.settings,
        ) { agentList, skillList, appSettings ->
            StudioUiState(
                agents = agentList,
                skills = skillList,
                tools = tools,
                toolEnabled = appSettings.toolEnabled,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            StudioUiState(tools = tools),
        )

    fun deleteAgent(agentId: String) {
        viewModelScope.launch { deleteAgentUseCase(agentId) }
    }

    fun deleteSkill(skillId: String) {
        viewModelScope.launch { deleteSkillUseCase(skillId) }
    }

    fun setSkillEnabled(skillId: String, enabled: Boolean) {
        viewModelScope.launch { skills.setEnabled(skillId, enabled) }
    }

    fun setToolEnabled(toolId: String, enabled: Boolean) {
        viewModelScope.launch { settings.setToolEnabled(toolId, enabled) }
    }
}
