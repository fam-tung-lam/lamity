package com.phamtunglam.lamity.feature.models.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phamtunglam.lamity.core.presentation.designSystem.components.SubScreenScaffold
import com.phamtunglam.lamity.feature.models.presentation.components.ModelConfigEditor
import com.phamtunglam.lamity.shared.resources.Res
import com.phamtunglam.lamity.shared.resources.config_note
import com.phamtunglam.lamity.shared.resources.model_config_title
import com.phamtunglam.lamity.shared.resources.reset_defaults
import com.phamtunglam.lamity.shared.resources.save
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ModelConfigScreen(
    modelId: String,
    onBack: () -> Unit,
    viewModel: ModelConfigViewModel = koinViewModel { parametersOf(modelId) },
) {
    val ui by viewModel.uiState.collectAsState()
    val model = ui.model

    // Navigation is driven by state: leave once saved, or if the model vanished.
    LaunchedEffect(ui.saved, ui.isLoading, model) {
        if (ui.saved || (!ui.isLoading && model == null)) onBack()
    }
    if (model == null) return

    SubScreenScaffold(title = "${stringResource(Res.string.model_config_title)} — ${model.name}", onBack = onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            ModelConfigEditor(config = ui.config, onChange = viewModel::setConfig)

            Text(
                stringResource(Res.string.config_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { viewModel.resetToDefaults() }) {
                    Text(stringResource(Res.string.reset_defaults))
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = viewModel::save) { Text(stringResource(Res.string.save)) }
            }
        }
    }
}
