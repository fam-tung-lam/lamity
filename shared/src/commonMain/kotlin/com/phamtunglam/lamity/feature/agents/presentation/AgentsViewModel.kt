package com.phamtunglam.lamity.feature.agents.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phamtunglam.lamity.feature.agents.data.AgentsRepository
import com.phamtunglam.lamity.feature.agents.domain.Agent
import com.phamtunglam.lamity.feature.agents.domain.DeleteAgentUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AgentsUiState(val agents: List<Agent> = emptyList())

class AgentsViewModel(agents: AgentsRepository, private val deleteAgentUseCase: DeleteAgentUseCase) : ViewModel() {
    val uiState: StateFlow<AgentsUiState> =
        agents.agents
            .map { AgentsUiState(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AgentsUiState())

    fun deleteAgent(agentId: String) {
        viewModelScope.launch { deleteAgentUseCase(agentId) }
    }
}
