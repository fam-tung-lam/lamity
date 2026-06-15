package com.phamtunglam.lamity.feature.studio.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phamtunglam.lamity.core.domain.tools.BuiltinTool
import com.phamtunglam.lamity.core.presentation.designSystem.components.SubScreenScaffold
import com.phamtunglam.lamity.feature.studio.domain.Skill
import com.phamtunglam.lamity.shared.resources.Res
import com.phamtunglam.lamity.shared.resources.agent_description
import com.phamtunglam.lamity.shared.resources.agent_name
import com.phamtunglam.lamity.shared.resources.attached_skills
import com.phamtunglam.lamity.shared.resources.attached_tools
import com.phamtunglam.lamity.shared.resources.disabled_suffix
import com.phamtunglam.lamity.shared.resources.name_required
import com.phamtunglam.lamity.shared.resources.new_agent
import com.phamtunglam.lamity.shared.resources.save
import com.phamtunglam.lamity.shared.resources.system_prompt
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AgentEditScreen(
    agentId: String?,
    onBack: () -> Unit,
    viewModel: AgentEditViewModel = koinViewModel { parametersOf(agentId) },
) {
    val ui by viewModel.uiState.collectAsState()
    val skills by viewModel.availableSkills.collectAsState()

    // Navigation is driven by state: the ViewModel flags the save as done.
    LaunchedEffect(ui.saved) {
        if (ui.saved) onBack()
    }

    SubScreenScaffold(
        title = ui.existingName ?: stringResource(Res.string.new_agent),
        onBack = onBack,
        actions = {
            TextButton(onClick = viewModel::save, enabled = ui.canSave) { Text(stringResource(Res.string.save)) }
        },
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AgentTextFields(ui = ui, viewModel = viewModel)

            AttachedToolsSection(
                tools = viewModel.availableTools,
                selectedIds = ui.selectedToolIds,
                onToggle = viewModel::toggleTool,
            )

            AttachedSkillsSection(
                skills = skills,
                selectedIds = ui.selectedSkillIds,
                onToggle = viewModel::toggleSkill,
            )

            Button(
                onClick = viewModel::save,
                enabled = ui.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(Res.string.save)) }
        }
    }
}

@Composable
private fun AgentTextFields(ui: AgentEditUiState, viewModel: AgentEditViewModel) {
    OutlinedTextField(
        value = ui.name,
        onValueChange = viewModel::setName,
        label = { Text(stringResource(Res.string.agent_name)) },
        supportingText =
            if (ui.name.isBlank()) {
                { Text(stringResource(Res.string.name_required)) }
            } else {
                null
            },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = ui.description,
        onValueChange = viewModel::setDescription,
        label = { Text(stringResource(Res.string.agent_description)) },
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = ui.systemPrompt,
        onValueChange = viewModel::setSystemPrompt,
        label = { Text(stringResource(Res.string.system_prompt)) },
        minLines = 4,
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AttachedToolsSection(tools: List<BuiltinTool>, selectedIds: List<String>, onToggle: (String) -> Unit) {
    Text(stringResource(Res.string.attached_tools), style = MaterialTheme.typography.titleSmall)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tools.forEach { tool ->
            FilterChip(
                selected = tool.id in selectedIds,
                onClick = { onToggle(tool.id) },
                label = { Text(tool.displayName) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AttachedSkillsSection(skills: List<Skill>, selectedIds: List<String>, onToggle: (String) -> Unit) {
    Text(stringResource(Res.string.attached_skills), style = MaterialTheme.typography.titleSmall)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        skills.forEach { skill ->
            FilterChip(
                selected = skill.id in selectedIds,
                onClick = { onToggle(skill.id) },
                label = {
                    Text(
                        skill.name + if (!skill.enabled) " ${stringResource(Res.string.disabled_suffix)}" else "",
                    )
                },
            )
        }
    }
}
