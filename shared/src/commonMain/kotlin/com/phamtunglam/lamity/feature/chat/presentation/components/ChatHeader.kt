package com.phamtunglam.lamity.feature.chat.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phamtunglam.lamity.core.presentation.designSystem.components.SimpleDropdown
import com.phamtunglam.lamity.core.presentation.i18n.LocalStrings
import com.phamtunglam.lamity.feature.models.domain.LlmModel
import com.phamtunglam.lamity.feature.studio.domain.Agent

/** Agent and model pickers plus the new-chat action. */
@Composable
internal fun ChatHeader(
    agents: List<Agent>,
    models: List<LlmModel>,
    selectedAgentId: String?,
    selectedModelId: String?,
    onSelectAgent: (String?) -> Unit,
    onSelectModel: (String) -> Unit,
    onNewChat: () -> Unit,
) {
    val str = LocalStrings.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SimpleDropdown(
            label = str.agentLabel,
            options = listOf<Pair<String?, String>>(null to str.noAgent) +
                agents.map { it.id as String? to it.name },
            selectedId = selectedAgentId,
            onSelect = onSelectAgent,
            modifier = Modifier.weight(1f, fill = false),
        )
        SimpleDropdown(
            label = str.modelLabel,
            options = models.map { it.id as String? to it.name },
            selectedId = selectedModelId,
            onSelect = { it?.let(onSelectModel) },
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.weight(0.01f))
        IconButton(onClick = onNewChat) {
            Icon(Icons.Default.Add, contentDescription = str.newChat)
        }
    }
}
