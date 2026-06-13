package com.phamtunglam.lamity.feature.studio.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phamtunglam.lamity.core.tools.BuiltinTool
import com.phamtunglam.lamity.core.tools.ToolRegistry
import com.phamtunglam.lamity.feature.studio.data.AgentsRepository
import com.phamtunglam.lamity.feature.studio.data.SkillsRepository
import com.phamtunglam.lamity.feature.studio.domain.SaveAgentUseCase
import com.phamtunglam.lamity.feature.studio.domain.Skill
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AgentEditUiState(
    val existingName: String? = null,
    val name: String = "",
    val description: String = "",
    val systemPrompt: String = "",
    val selectedToolIds: List<String> = emptyList(),
    val selectedSkillIds: List<String> = emptyList(),
    /** Set once the agent has been persisted; the UI navigates back on it. */
    val saved: Boolean = false,
) {
    val canSave: Boolean get() = name.isNotBlank()
}

class AgentEditViewModel(
    private val agentId: String?,
    agents: AgentsRepository,
    skills: SkillsRepository,
    registry: ToolRegistry,
    private val saveAgent: SaveAgentUseCase,
) : ViewModel() {

    val availableTools: List<BuiltinTool> = registry.userSelectable

    val availableSkills: StateFlow<List<Skill>> = skills.skills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), skills.skills.value)

    private val _uiState = MutableStateFlow(AgentEditUiState())
    val uiState: StateFlow<AgentEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            agents.awaitLoaded()
            agents.byId(agentId)?.let { agent ->
                _uiState.value = AgentEditUiState(
                    existingName = agent.name,
                    name = agent.name,
                    description = agent.description,
                    systemPrompt = agent.systemPrompt,
                    selectedToolIds = agent.toolIds,
                    selectedSkillIds = agent.skillIds,
                )
            }
        }
    }

    fun setName(value: String) = _uiState.update { it.copy(name = value) }
    fun setDescription(value: String) = _uiState.update { it.copy(description = value) }
    fun setSystemPrompt(value: String) = _uiState.update { it.copy(systemPrompt = value) }

    fun toggleTool(toolId: String) = _uiState.update {
        it.copy(
            selectedToolIds = if (toolId in it.selectedToolIds) it.selectedToolIds - toolId
            else it.selectedToolIds + toolId,
        )
    }

    fun toggleSkill(skillId: String) = _uiState.update {
        it.copy(
            selectedSkillIds = if (skillId in it.selectedSkillIds) it.selectedSkillIds - skillId
            else it.selectedSkillIds + skillId,
        )
    }

    fun save() {
        val s = _uiState.value
        if (!s.canSave) return
        viewModelScope.launch {
            val agent = saveAgent(
                id = agentId,
                name = s.name,
                description = s.description,
                systemPrompt = s.systemPrompt,
                toolIds = s.selectedToolIds,
                skillIds = s.selectedSkillIds,
            )
            if (agent != null) _uiState.update { it.copy(saved = true) }
        }
    }
}
