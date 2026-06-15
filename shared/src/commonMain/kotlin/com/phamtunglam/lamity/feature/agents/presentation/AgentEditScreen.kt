package com.phamtunglam.lamity.feature.agents.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phamtunglam.lamity.core.presentation.designSystem.components.SubScreenScaffold
import com.phamtunglam.lamity.feature.models.domain.LlmModel
import com.phamtunglam.lamity.feature.models.presentation.components.ModelConfigEditor
import com.phamtunglam.lamity.feature.skills.domain.Skill
import com.phamtunglam.lamity.feature.tools.domain.AppTool
import com.phamtunglam.lamity.shared.resources.Res
import com.phamtunglam.lamity.shared.resources.agent_description
import com.phamtunglam.lamity.shared.resources.agent_model
import com.phamtunglam.lamity.shared.resources.agent_name
import com.phamtunglam.lamity.shared.resources.attached_skills
import com.phamtunglam.lamity.shared.resources.attached_tools
import com.phamtunglam.lamity.shared.resources.name_required
import com.phamtunglam.lamity.shared.resources.new_agent
import com.phamtunglam.lamity.shared.resources.save
import com.phamtunglam.lamity.shared.resources.step_basics
import com.phamtunglam.lamity.shared.resources.step_config
import com.phamtunglam.lamity.shared.resources.step_model
import com.phamtunglam.lamity.shared.resources.step_skills
import com.phamtunglam.lamity.shared.resources.step_tools
import com.phamtunglam.lamity.shared.resources.system_prompt
import com.phamtunglam.lamity.shared.resources.wizard_back
import com.phamtunglam.lamity.shared.resources.wizard_next
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
    val models by viewModel.availableModels.collectAsState()

    // Navigation is driven by state: the ViewModel flags the save as done.
    LaunchedEffect(ui.saved) {
        if (ui.saved) onBack()
    }

    SubScreenScaffold(
        title = ui.existingName ?: stringResource(Res.string.new_agent),
        onBack = { if (ui.isFirstStep) onBack() else viewModel.back() },
    ) {
        Column(Modifier.fillMaxSize()) {
            StepHeader(stepIndex = ui.stepIndex, total = ui.steps.size, step = ui.currentStep)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    WizardStepBody(ui = ui, viewModel = viewModel, skills = skills, models = models)
                }
            }
            HorizontalDivider()
            WizardActions(ui = ui, viewModel = viewModel)
        }
    }
}

@Composable
private fun StepHeader(stepIndex: Int, total: Int, step: AgentWizardStep) {
    Text(
        "${stepIndex + 1}/$total • ${stepTitle(step)}",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun stepTitle(step: AgentWizardStep): String =
    when (step) {
        AgentWizardStep.BASICS -> stringResource(Res.string.step_basics)
        AgentWizardStep.MODEL -> stringResource(Res.string.step_model)
        AgentWizardStep.CONFIG -> stringResource(Res.string.step_config)
        AgentWizardStep.SKILLS -> stringResource(Res.string.step_skills)
        AgentWizardStep.TOOLS -> stringResource(Res.string.step_tools)
    }

@Composable
private fun WizardStepBody(
    ui: AgentEditUiState,
    viewModel: AgentEditViewModel,
    skills: List<Skill>,
    models: List<LlmModel>,
) {
    when (ui.currentStep) {
        AgentWizardStep.BASICS -> {
            AgentTextFields(ui = ui, viewModel = viewModel)
        }

        AgentWizardStep.MODEL -> {
            ModelStep(models = models, selectedId = ui.modelId, onSelect = viewModel::setModel)
        }

        AgentWizardStep.CONFIG -> {
            ModelConfigEditor(config = ui.modelConfig, onChange = viewModel::setModelConfig)
        }

        AgentWizardStep.SKILLS -> {
            AttachedSkillsSection(skills = skills, selectedIds = ui.selectedSkillIds, onToggle = viewModel::toggleSkill)
        }

        AgentWizardStep.TOOLS -> {
            AttachedToolsSection(
                tools = viewModel.availableTools,
                selectedIds = ui.selectedToolIds,
                onToggle = viewModel::toggleTool,
            )
        }
    }
}

@Composable
private fun WizardActions(ui: AgentEditUiState, viewModel: AgentEditViewModel) {
    Row(
        Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!ui.isFirstStep) {
            OutlinedButton(onClick = viewModel::back) { Text(stringResource(Res.string.wizard_back)) }
        }
        Spacer(Modifier.weight(1f))
        if (ui.isLastStep) {
            Button(onClick = viewModel::save, enabled = ui.canSave) { Text(stringResource(Res.string.save)) }
        } else {
            Button(onClick = viewModel::next, enabled = ui.canAdvance) {
                Text(stringResource(Res.string.wizard_next))
            }
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

@Composable
private fun ModelStep(models: List<LlmModel>, selectedId: String?, onSelect: (String) -> Unit) {
    Text(stringResource(Res.string.agent_model), style = MaterialTheme.typography.titleSmall)
    models.forEach { model ->
        Card(onClick = { onSelect(model.id) }, modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.padding(start = 4.dp, end = 14.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = model.id == selectedId, onClick = { onSelect(model.id) })
                Column(Modifier.weight(1f)) {
                    Text(model.name, style = MaterialTheme.typography.titleSmall)
                    if (model.description.isNotBlank()) {
                        Text(
                            model.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AttachedToolsSection(tools: List<AppTool>, selectedIds: List<String>, onToggle: (String) -> Unit) {
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
                label = { Text(skill.name) },
            )
        }
    }
}
