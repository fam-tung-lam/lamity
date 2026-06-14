package com.phamtunglam.lamity.feature.studio.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phamtunglam.lamity.core.domain.tools.BuiltinTool
import com.phamtunglam.lamity.core.domain.tools.ToolRegistry
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
    val tools: List<BuiltinTool> = emptyList(),
    val toolEnabled: Map<String, Boolean> = emptyMap(),
)

class StudioViewModel(
    agents: AgentsRepository,
    private val skills: SkillsRepository,
    private val settings: SettingsRepository,
    registry: ToolRegistry,
    private val deleteAgentUseCase: DeleteAgentUseCase,
    private val deleteSkillUseCase: DeleteSkillUseCase,
) : ViewModel() {

    val uiState: StateFlow<StudioUiState> = combine(
        agents.agents,
        skills.skills,
        settings.settings,
    ) { agentList, skillList, appSettings ->
        StudioUiState(
            agents = agentList,
            skills = skillList,
            tools = registry.userSelectable,
            toolEnabled = appSettings.toolEnabled,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        StudioUiState(tools = registry.userSelectable),
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
