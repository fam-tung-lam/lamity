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
import com.phamtunglam.lamity.core.designsystem.components.SubScreenScaffold
import com.phamtunglam.lamity.core.i18n.LocalStrings
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgentEditScreen(
    agentId: String?,
    onBack: () -> Unit,
    viewModel: AgentEditViewModel = koinViewModel { parametersOf(agentId) },
) {
    val str = LocalStrings.current
    val ui by viewModel.uiState.collectAsState()
    val skills by viewModel.availableSkills.collectAsState()

    // Navigation is driven by state: the ViewModel flags the save as done.
    LaunchedEffect(ui.saved) {
        if (ui.saved) onBack()
    }

    SubScreenScaffold(
        title = ui.existingName ?: str.newAgent,
        onBack = onBack,
        actions = {
            TextButton(onClick = viewModel::save, enabled = ui.canSave) { Text(str.save) }
        },
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(
                value = ui.name,
                onValueChange = viewModel::setName,
                label = { Text(str.agentName) },
                supportingText = if (ui.name.isBlank()) {
                    { Text(str.nameRequired) }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = ui.description,
                onValueChange = viewModel::setDescription,
                label = { Text(str.agentDescription) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = ui.systemPrompt,
                onValueChange = viewModel::setSystemPrompt,
                label = { Text(str.systemPrompt) },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(str.attachedTools, style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                viewModel.availableTools.forEach { tool ->
                    FilterChip(
                        selected = tool.id in ui.selectedToolIds,
                        onClick = { viewModel.toggleTool(tool.id) },
                        label = { Text(tool.displayName) },
                    )
                }
            }

            Text(str.attachedSkills, style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                skills.forEach { skill ->
                    FilterChip(
                        selected = skill.id in ui.selectedSkillIds,
                        onClick = { viewModel.toggleSkill(skill.id) },
                        label = {
                            Text(skill.name + if (!skill.enabled) " ${str.disabledSuffix}" else "")
                        },
                    )
                }
            }

            Button(
                onClick = viewModel::save,
                enabled = ui.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(str.save) }
        }
    }
}
