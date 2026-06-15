package com.phamtunglam.lamity.feature.agents.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phamtunglam.lamity.feature.agents.data.AgentsRepository
import com.phamtunglam.lamity.feature.agents.domain.SaveAgentUseCase
import com.phamtunglam.lamity.feature.models.data.ModelsRepository
import com.phamtunglam.lamity.feature.models.domain.LlmModel
import com.phamtunglam.lamity.feature.models.domain.ModelConfig
import com.phamtunglam.lamity.feature.skills.data.SkillsRepository
import com.phamtunglam.lamity.feature.skills.domain.Skill
import com.phamtunglam.lamity.feature.tools.domain.AppTool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Ordered wizard steps; SKILLS/TOOLS are only shown for tool-capable models. */
enum class AgentWizardStep { BASICS, MODEL, CONFIG, SKILLS, TOOLS }

data class AgentEditUiState(
    val existingName: String? = null,
    val stepIndex: Int = 0,
    val name: String = "",
    val description: String = "",
    val systemPrompt: String = "",
    val modelId: String? = null,
    val modelConfig: ModelConfig = ModelConfig(),
    val selectedModelSupportsTools: Boolean = false,
    val selectedToolIds: List<String> = emptyList(),
    val selectedSkillIds: List<String> = emptyList(),
    /** Set once the agent has been persisted; the UI navigates back on it. */
    val saved: Boolean = false,
) {
    val steps: List<AgentWizardStep>
        get() =
            if (selectedModelSupportsTools) {
                AgentWizardStep.entries
            } else {
                listOf(AgentWizardStep.BASICS, AgentWizardStep.MODEL, AgentWizardStep.CONFIG)
            }

    val currentStep: AgentWizardStep get() = steps[stepIndex.coerceIn(0, steps.lastIndex)]
    val isFirstStep: Boolean get() = stepIndex <= 0
    val isLastStep: Boolean get() = stepIndex >= steps.lastIndex
    val canSave: Boolean get() = name.isNotBlank() && modelId != null
    val canAdvance: Boolean
        get() =
            when (currentStep) {
                AgentWizardStep.BASICS -> name.isNotBlank()
                AgentWizardStep.MODEL -> modelId != null
                else -> true
            }
}

class AgentEditViewModel(
    private val agentId: String?,
    agents: AgentsRepository,
    skills: SkillsRepository,
    private val models: ModelsRepository,
    tools: List<AppTool>,
    private val saveAgent: SaveAgentUseCase,
) : ViewModel() {
    val availableTools: List<AppTool> = tools

    val availableSkills: StateFlow<List<Skill>> =
        skills.skills
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), skills.skills.value)

    val availableModels: StateFlow<List<LlmModel>> =
        models.models
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), models.models.value)

    private val _uiState = MutableStateFlow(AgentEditUiState())
    val uiState: StateFlow<AgentEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            agents.awaitLoaded()
            models.awaitLoaded()
            agents.byId(agentId)?.let { agent ->
                val model = models.byId(agent.modelId)
                _uiState.value =
                    AgentEditUiState(
                        existingName = agent.name,
                        name = agent.name,
                        description = agent.description,
                        systemPrompt = agent.systemPrompt,
                        modelId = agent.modelId,
                        modelConfig = agent.modelConfig ?: model?.config ?: ModelConfig(),
                        selectedModelSupportsTools = model?.supportsTools ?: false,
                        selectedToolIds = agent.toolIds,
                        selectedSkillIds = agent.skillIds,
                    )
            }
        }
    }

    fun setName(value: String) = _uiState.update { it.copy(name = value) }

    fun setDescription(value: String) = _uiState.update { it.copy(description = value) }

    fun setSystemPrompt(value: String) = _uiState.update { it.copy(systemPrompt = value) }

    fun setModel(modelId: String) {
        val model = models.byId(modelId) ?: return
        _uiState.update {
            it
                .copy(
                    modelId = modelId,
                    // Adopt the model's saved config as the starting point for the override.
                    modelConfig = model.config,
                    selectedModelSupportsTools = model.supportsTools,
                    // Drop attachments the model can't use; clamp the step if the list shrank.
                    selectedToolIds = if (model.supportsTools) it.selectedToolIds else emptyList(),
                    selectedSkillIds = if (model.supportsTools) it.selectedSkillIds else emptyList(),
                ).let { s -> s.copy(stepIndex = s.stepIndex.coerceAtMost(s.steps.lastIndex)) }
        }
    }

    fun setModelConfig(config: ModelConfig) = _uiState.update { it.copy(modelConfig = config) }

    fun toggleTool(toolId: String) =
        _uiState.update {
            it.copy(
                selectedToolIds =
                    if (toolId in it.selectedToolIds) it.selectedToolIds - toolId else it.selectedToolIds + toolId,
            )
        }

    fun toggleSkill(skillId: String) =
        _uiState.update {
            it.copy(
                selectedSkillIds =
                    if (skillId in it.selectedSkillIds) {
                        it.selectedSkillIds - skillId
                    } else {
                        it.selectedSkillIds + skillId
                    },
            )
        }

    fun next() = _uiState.update { if (it.isLastStep) it else it.copy(stepIndex = it.stepIndex + 1) }

    fun back() = _uiState.update { if (it.isFirstStep) it else it.copy(stepIndex = it.stepIndex - 1) }

    fun save() {
        val s = _uiState.value
        if (!s.canSave) return
        val modelId = s.modelId ?: return
        viewModelScope.launch {
            val agent =
                saveAgent(
                    id = agentId,
                    name = s.name,
                    description = s.description,
                    systemPrompt = s.systemPrompt,
                    toolIds = s.selectedToolIds,
                    skillIds = s.selectedSkillIds,
                    modelId = modelId,
                    modelConfig = s.modelConfig,
                )
            if (agent != null) _uiState.update { it.copy(saved = true) }
        }
    }
}
