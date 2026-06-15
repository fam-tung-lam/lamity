package com.phamtunglam.lamity.feature.skills.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phamtunglam.lamity.feature.skills.data.SkillsRepository
import com.phamtunglam.lamity.feature.skills.domain.SaveSkillUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SkillEditUiState(
    val existingName: String? = null,
    val name: String = "",
    val description: String = "",
    val instructions: String = "",
    /** Set once the skill has been persisted; the UI navigates back on it. */
    val saved: Boolean = false,
) {
    val canSave: Boolean get() = name.isNotBlank()
}

class SkillEditViewModel(
    private val skillId: String?,
    skills: SkillsRepository,
    private val saveSkill: SaveSkillUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SkillEditUiState())
    val uiState: StateFlow<SkillEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            skills.awaitLoaded()
            skills.byId(skillId)?.let { skill ->
                _uiState.value =
                    SkillEditUiState(
                        existingName = skill.name,
                        name = skill.name,
                        description = skill.description,
                        instructions = skill.instructions,
                    )
            }
        }
    }

    fun setName(value: String) = _uiState.update { it.copy(name = value) }

    fun setDescription(value: String) = _uiState.update { it.copy(description = value) }

    fun setInstructions(value: String) = _uiState.update { it.copy(instructions = value) }

    fun save() {
        val s = _uiState.value
        if (!s.canSave) return
        viewModelScope.launch {
            val skill =
                saveSkill(
                    id = skillId,
                    name = s.name,
                    description = s.description,
                    instructions = s.instructions,
                )
            if (skill != null) _uiState.update { it.copy(saved = true) }
        }
    }
}
