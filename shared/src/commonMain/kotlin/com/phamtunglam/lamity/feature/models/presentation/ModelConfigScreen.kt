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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phamtunglam.lamity.core.presentation.designSystem.components.LabeledSlider
import com.phamtunglam.lamity.core.presentation.designSystem.components.SubScreenScaffold
import com.phamtunglam.lamity.core.presentation.i18n.LocalStrings
import com.phamtunglam.lamity.feature.models.domain.LlmBackend
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.roundToInt

@Composable
fun ModelConfigScreen(
    modelId: String,
    onBack: () -> Unit,
    viewModel: ModelConfigViewModel = koinViewModel { parametersOf(modelId) },
) {
    val str = LocalStrings.current
    val ui by viewModel.uiState.collectAsState()
    val model = ui.model

    // Navigation is driven by state: leave once saved, or if the model vanished.
    LaunchedEffect(ui.saved, ui.isLoading, model) {
        if (ui.saved || (!ui.isLoading && model == null)) onBack()
    }
    if (model == null) return

    SubScreenScaffold(title = "${str.modelConfigTitle} — ${model.name}", onBack = onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column {
                Text(str.backend, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.padding(2.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    LlmBackend.entries.forEachIndexed { index, entry ->
                        SegmentedButton(
                            selected = ui.backend == entry,
                            onClick = { viewModel.setBackend(entry) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = LlmBackend.entries.size,
                            ),
                        ) { Text(entry.name) }
                    }
                }
            }

            LabeledSlider(
                label = str.maxTokens,
                value = ui.maxTokens,
                valueText = ((ui.maxTokens / 256f).roundToInt() * 256).toString(),
                range = 256f..8192f,
                steps = 30,
                onChange = { viewModel.setMaxTokens(it) },
            )
            LabeledSlider(
                label = str.topK,
                value = ui.topK,
                valueText = ui.topK.roundToInt().toString(),
                range = 1f..128f,
                steps = 126,
                onChange = { viewModel.setTopK(it) },
            )
            LabeledSlider(
                label = str.topP,
                value = ui.topP,
                valueText = fmt2(ui.topP),
                range = 0.05f..1f,
                steps = 18,
                onChange = { viewModel.setTopP(it) },
            )
            LabeledSlider(
                label = str.temperature,
                value = ui.temperature,
                valueText = fmt2(ui.temperature),
                range = 0f..2f,
                steps = 39,
                onChange = { viewModel.setTemperature(it) },
            )

            Text(
                str.configNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { viewModel.resetToDefaults() }) {
                    Text(str.resetDefaults)
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = viewModel::save) { Text(str.save) }
            }
        }
    }
}

private fun fmt2(value: Float): String = ((value * 100).roundToInt() / 100.0).toString()
