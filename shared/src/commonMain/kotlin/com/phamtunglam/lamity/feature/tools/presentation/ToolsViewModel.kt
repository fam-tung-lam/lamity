package com.phamtunglam.lamity.feature.tools.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phamtunglam.lamity.feature.settings.data.SettingsRepository
import com.phamtunglam.lamity.feature.tools.domain.AppTool
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ToolsUiState(val tools: List<AppTool> = emptyList(), val toolEnabled: Map<String, Boolean> = emptyMap())

class ToolsViewModel(private val tools: List<AppTool>, private val settings: SettingsRepository) : ViewModel() {
    val uiState: StateFlow<ToolsUiState> =
        settings.settings
            .map { ToolsUiState(tools = tools, toolEnabled = it.toolEnabled) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ToolsUiState(tools = tools))

    fun setToolEnabled(toolId: String, enabled: Boolean) {
        viewModelScope.launch { settings.setToolEnabled(toolId, enabled) }
    }
}
