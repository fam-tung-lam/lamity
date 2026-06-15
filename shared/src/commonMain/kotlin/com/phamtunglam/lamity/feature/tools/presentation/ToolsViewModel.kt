package com.phamtunglam.lamity.feature.tools.presentation

import androidx.lifecycle.ViewModel
import com.phamtunglam.lamity.feature.tools.domain.AppTool

data class ToolsUiState(val tools: List<AppTool> = emptyList())

/**
 * Read-only catalog of the built-in tools. Tools are no longer globally toggled — a tool is used by
 * a chat only when the active agent attaches it (see the agent editor).
 */
class ToolsViewModel(tools: List<AppTool>) : ViewModel() {
    val uiState: ToolsUiState = ToolsUiState(tools = tools)
}
