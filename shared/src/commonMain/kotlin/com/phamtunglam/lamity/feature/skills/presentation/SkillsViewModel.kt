package com.phamtunglam.lamity.feature.skills.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phamtunglam.lamity.feature.skills.data.SkillsRepository
import com.phamtunglam.lamity.feature.skills.domain.DeleteSkillUseCase
import com.phamtunglam.lamity.feature.skills.domain.Skill
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SkillsUiState(val skills: List<Skill> = emptyList())

class SkillsViewModel(private val skills: SkillsRepository, private val deleteSkillUseCase: DeleteSkillUseCase) :
    ViewModel() {
    val uiState: StateFlow<SkillsUiState> =
        skills.skills
            .map { SkillsUiState(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SkillsUiState())

    fun deleteSkill(skillId: String) {
        viewModelScope.launch { deleteSkillUseCase(skillId) }
    }
}
