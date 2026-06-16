package com.phamtunglam.lamity.feature.chat.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phamtunglam.lamity.feature.chat.domain.skills.Skill
import com.phamtunglam.lamity.feature.chat.domain.tools.AppTool
import com.phamtunglam.lamity.feature.llmModels.domain.LlmModel
import com.phamtunglam.lamity.feature.llmModels.domain.ModelConfig
import com.phamtunglam.lamity.feature.llmModels.presentation.components.ModelConfigEditor
import com.phamtunglam.lamity.shared.resources.Res
import com.phamtunglam.lamity.shared.resources.attached_skills
import com.phamtunglam.lamity.shared.resources.attached_tools
import com.phamtunglam.lamity.shared.resources.custom_system_prompt
import com.phamtunglam.lamity.shared.resources.model_no_tools
import com.phamtunglam.lamity.shared.resources.section_model_config
import com.phamtunglam.lamity.shared.resources.section_system_prompt
import org.jetbrains.compose.resources.stringResource

/**
 * Per-chat settings: an optional system prompt, the inference parameters and on/off toggles for the
 * built-in tools and skills, split into divider-separated sections. Everything here is in-memory for
 * the chat (not persisted); the model itself is chosen on the Models screen. The tools/skills section
 * is replaced with a notice for models without tool support.
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
    // Fully expanded, full-height sheet: skipPartiallyExpanded opens it expanded, fillMaxSize makes it
    // span the screen, and statusBarsPadding keeps the top (with its rounded corners) below the status
    // bar. The default content insets already lift content above the navigation bar.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.statusBarsPadding().fillMaxSize(),
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // System prompt
            Text(stringResource(Res.string.section_system_prompt), style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = customSystemPrompt.orEmpty(),
                onValueChange = onSetSystemPrompt,
                label = { Text(stringResource(Res.string.custom_system_prompt)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider()

            // Model configs
            Text(stringResource(Res.string.section_model_config), style = MaterialTheme.typography.titleSmall)
            ModelConfigEditor(config = runtimeConfig, onChange = onSetConfig)

            HorizontalDivider()

            // Tools & skills
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
