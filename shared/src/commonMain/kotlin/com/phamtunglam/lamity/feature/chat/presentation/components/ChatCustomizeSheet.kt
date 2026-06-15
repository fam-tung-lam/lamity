package com.phamtunglam.lamity.feature.chat.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phamtunglam.lamity.core.presentation.designSystem.components.SimpleDropdown
import com.phamtunglam.lamity.feature.agents.domain.Agent
import com.phamtunglam.lamity.feature.models.domain.LlmModel
import com.phamtunglam.lamity.feature.skills.domain.Skill
import com.phamtunglam.lamity.feature.tools.domain.AppTool
import com.phamtunglam.lamity.shared.resources.Res
import com.phamtunglam.lamity.shared.resources.agent_label
import com.phamtunglam.lamity.shared.resources.attached_skills
import com.phamtunglam.lamity.shared.resources.attached_tools
import com.phamtunglam.lamity.shared.resources.custom_system_prompt
import com.phamtunglam.lamity.shared.resources.customize_chat
import com.phamtunglam.lamity.shared.resources.model_label
import com.phamtunglam.lamity.shared.resources.no_agent
import org.jetbrains.compose.resources.stringResource

/** Per-chat customization: pick an agent, or run agent-less with a custom model, tools, skills, prompt. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun ChatCustomizeSheet(
    agents: List<Agent>,
    models: List<LlmModel>,
    skills: List<Skill>,
    tools: List<AppTool>,
    selectedAgentId: String?,
    selectedModelId: String?,
    customToolIds: List<String>?,
    customSkillIds: List<String>?,
    customSystemPrompt: String?,
    onSelectAgent: (String?) -> Unit,
    onSelectModel: (String) -> Unit,
    onSetSystemPrompt: (String) -> Unit,
    onToggleTool: (String) -> Unit,
    onToggleSkill: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(Res.string.customize_chat), style = MaterialTheme.typography.titleMedium)

            SimpleDropdown(
                label = stringResource(Res.string.agent_label),
                options =
                    listOf<Pair<String?, String>>(null to stringResource(Res.string.no_agent)) +
                        agents.map { it.id as String? to it.name },
                selectedId = selectedAgentId,
                onSelect = onSelectAgent,
            )

            // Agent-less: expose the per-chat model, prompt, tools and skills.
            if (selectedAgentId == null) {
                SimpleDropdown(
                    label = stringResource(Res.string.model_label),
                    options = models.map { it.id as String? to it.name },
                    selectedId = selectedModelId,
                    onSelect = { it?.let(onSelectModel) },
                )
                OutlinedTextField(
                    value = customSystemPrompt.orEmpty(),
                    onValueChange = onSetSystemPrompt,
                    label = { Text(stringResource(Res.string.custom_system_prompt)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )

                val effectiveToolIds = customToolIds ?: tools.map { it.id }
                Text(stringResource(Res.string.attached_tools), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tools.forEach { tool ->
                        FilterChip(
                            selected = tool.id in effectiveToolIds,
                            onClick = { onToggleTool(tool.id) },
                            label = { Text(tool.displayName) },
                        )
                    }
                }

                val effectiveSkillIds = customSkillIds.orEmpty()
                Text(stringResource(Res.string.attached_skills), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    skills.filter { it.enabled }.forEach { skill ->
                        FilterChip(
                            selected = skill.id in effectiveSkillIds,
                            onClick = { onToggleSkill(skill.id) },
                            label = { Text(skill.name) },
                        )
                    }
                }
            }
        }
    }
}
