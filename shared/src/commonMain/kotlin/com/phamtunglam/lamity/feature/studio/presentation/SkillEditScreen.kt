package com.phamtunglam.lamity.feature.studio.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phamtunglam.lamity.core.presentation.designSystem.components.SubScreenScaffold
import com.phamtunglam.lamity.shared.resources.Res
import com.phamtunglam.lamity.shared.resources.enabled
import com.phamtunglam.lamity.shared.resources.name_required
import com.phamtunglam.lamity.shared.resources.new_skill
import com.phamtunglam.lamity.shared.resources.save
import com.phamtunglam.lamity.shared.resources.skill_description
import com.phamtunglam.lamity.shared.resources.skill_instructions
import com.phamtunglam.lamity.shared.resources.skill_name
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SkillEditScreen(
    skillId: String?,
    onBack: () -> Unit,
    viewModel: SkillEditViewModel = koinViewModel { parametersOf(skillId) },
) {
    val ui by viewModel.uiState.collectAsState()

    // Navigation is driven by state: the ViewModel flags the save as done.
    LaunchedEffect(ui.saved) {
        if (ui.saved) onBack()
    }

    SubScreenScaffold(
        title = ui.existingName ?: stringResource(Res.string.new_skill),
        onBack = onBack,
        actions = {
            TextButton(onClick = viewModel::save, enabled = ui.canSave) { Text(stringResource(Res.string.save)) }
        },
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(
                value = ui.name,
                onValueChange = viewModel::setName,
                label = { Text(stringResource(Res.string.skill_name)) },
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
                label = { Text(stringResource(Res.string.skill_description)) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = ui.instructions,
                onValueChange = viewModel::setInstructions,
                label = { Text(stringResource(Res.string.skill_instructions)) },
                minLines = 6,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.enabled), Modifier.weight(1f))
                Switch(checked = ui.enabled, onCheckedChange = viewModel::setEnabled)
            }
            Button(
                onClick = viewModel::save,
                enabled = ui.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(Res.string.save)) }
        }
    }
}
