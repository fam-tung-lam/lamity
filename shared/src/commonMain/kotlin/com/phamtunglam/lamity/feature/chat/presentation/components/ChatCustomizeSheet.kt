package com.phamtunglam.lamity.feature.chat.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phamtunglam.lamity.feature.models.domain.LlmModel
import com.phamtunglam.lamity.feature.models.domain.ModelConfig
import com.phamtunglam.lamity.feature.models.presentation.components.ModelConfigEditor
import com.phamtunglam.lamity.feature.skills.domain.Skill
import com.phamtunglam.lamity.feature.tools.domain.AppTool
import com.phamtunglam.lamity.shared.resources.Res
import com.phamtunglam.lamity.shared.resources.attached_skills
import com.phamtunglam.lamity.shared.resources.attached_tools
import com.phamtunglam.lamity.shared.resources.custom_system_prompt
import com.phamtunglam.lamity.shared.resources.customize_chat
import com.phamtunglam.lamity.shared.resources.model_no_tools
import org.jetbrains.compose.resources.stringResource

/**
 * Per-chat settings: inference parameters, an optional system prompt and on/off toggles for the
 * built-in tools and skills. Everything here is in-memory for the chat (not persisted); the model
 * itself is chosen on the Models screen. Tools and skills are hidden for models without tool support.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatSettingsSheet(
    model: LlmModel?,
    runtimeConfig: ModelConfig,
    customSystemPrompt: String?,
    tools: List<AppTool>,
    skills: List<Skill>,
    enabledToolIds: Set<String>,
    enabledSkillIds: Set<String>,
    onSetConfig: (ModelConfig) -> Unit,
    onSetSystemPrompt: (String) -> Unit,
    onToggleTool: (String, Boolean) -> Unit,
    onToggleSkill: (String, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(Res.string.customize_chat), style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = customSystemPrompt.orEmpty(),
                onValueChange = onSetSystemPrompt,
                label = { Text(stringResource(Res.string.custom_system_prompt)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            ModelConfigEditor(config = runtimeConfig, onChange = onSetConfig)

            HorizontalDivider()

            if (model?.supportsTools == false) {
                Text(
                    stringResource(Res.string.model_no_tools),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                if (tools.isNotEmpty()) {
                    Text(stringResource(Res.string.attached_tools), style = MaterialTheme.typography.titleSmall)
                    tools.forEach { tool ->
                        ToggleRow(
                            title = tool.displayName,
                            subtitle = tool.description,
                            checked = tool.id in enabledToolIds,
                            onCheckedChange = { onToggleTool(tool.id, it) },
                        )
                    }
                }
                if (skills.isNotEmpty()) {
                    Text(stringResource(Res.string.attached_skills), style = MaterialTheme.typography.titleSmall)
                    skills.forEach { skill ->
                        ToggleRow(
                            title = skill.name,
                            subtitle = skill.description,
                            checked = skill.id in enabledSkillIds,
                            onCheckedChange = { onToggleSkill(skill.id, it) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
