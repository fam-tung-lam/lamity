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
import com.phamtunglam.lamity.feature.models.domain.LlmBackend
import com.phamtunglam.lamity.shared.resources.Res
import com.phamtunglam.lamity.shared.resources.backend
import com.phamtunglam.lamity.shared.resources.config_note
import com.phamtunglam.lamity.shared.resources.max_tokens
import com.phamtunglam.lamity.shared.resources.model_config_title
import com.phamtunglam.lamity.shared.resources.reset_defaults
import com.phamtunglam.lamity.shared.resources.save
import com.phamtunglam.lamity.shared.resources.temperature
import com.phamtunglam.lamity.shared.resources.top_k
import com.phamtunglam.lamity.shared.resources.top_p
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.roundToInt

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
            BackendSelector(selected = ui.backend, onSelect = viewModel::setBackend)

            ModelParamSliders(
                maxTokens = ui.maxTokens,
                topK = ui.topK,
                topP = ui.topP,
                temperature = ui.temperature,
                viewModel = viewModel,
            )

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

@Composable
private fun BackendSelector(selected: LlmBackend, onSelect: (LlmBackend) -> Unit) {
    Column {
        Text(stringResource(Res.string.backend), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.padding(2.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            LlmBackend.entries.forEachIndexed { index, entry ->
                SegmentedButton(
                    selected = selected == entry,
                    onClick = { onSelect(entry) },
                    shape =
                        SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = LlmBackend.entries.size,
                        ),
                ) { Text(entry.name) }
            }
        }
    }
}

@Composable
private fun ModelParamSliders(
    maxTokens: Float,
    topK: Float,
    topP: Float,
    temperature: Float,
    viewModel: ModelConfigViewModel,
) {
    LabeledSlider(
        label = stringResource(Res.string.max_tokens),
        value = maxTokens,
        valueText = ((maxTokens / 256f).roundToInt() * 256).toString(),
        range = 256f..8192f,
        steps = 30,
        onChange = { viewModel.setMaxTokens(it) },
    )
    LabeledSlider(
        label = stringResource(Res.string.top_k),
        value = topK,
        valueText = topK.roundToInt().toString(),
        range = 1f..128f,
        steps = 126,
        onChange = { viewModel.setTopK(it) },
    )
    LabeledSlider(
        label = stringResource(Res.string.top_p),
        value = topP,
        valueText = fmt2(topP),
        range = 0.05f..1f,
        steps = 18,
        onChange = { viewModel.setTopP(it) },
    )
    LabeledSlider(
        label = stringResource(Res.string.temperature),
        value = temperature,
        valueText = fmt2(temperature),
        range = 0f..2f,
        steps = 39,
        onChange = { viewModel.setTemperature(it) },
    )
}

private const val TWO_DECIMAL_PLACES = 100.0

private fun fmt2(value: Float): String = ((value * TWO_DECIMAL_PLACES).roundToInt() / TWO_DECIMAL_PLACES).toString()
