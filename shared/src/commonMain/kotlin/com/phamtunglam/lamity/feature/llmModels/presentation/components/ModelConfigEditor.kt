package com.phamtunglam.lamity.feature.llmModels.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phamtunglam.lamity.core.presentation.designSystem.components.LabeledSlider
import com.phamtunglam.lamity.feature.llmModels.domain.LlmBackend
import com.phamtunglam.lamity.feature.llmModels.domain.ModelConfig
import com.phamtunglam.lamity.shared.resources.Res
import com.phamtunglam.lamity.shared.resources.backend
import com.phamtunglam.lamity.shared.resources.max_tokens
import com.phamtunglam.lamity.shared.resources.temperature
import com.phamtunglam.lamity.shared.resources.top_k
import com.phamtunglam.lamity.shared.resources.top_p
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/**
 * Stateless editor for a [ModelConfig] (backend + sampling sliders). Emits already-snapped configs
 * so callers can store the value directly. Reused by the model-config screen, the agent wizard, and
 * the chat customization sheet.
 */
@Composable
fun ModelConfigEditor(config: ModelConfig, onChange: (ModelConfig) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        BackendSelector(selected = config.backend, onSelect = { onChange(config.copy(backend = it)) })
        LabeledSlider(
            label = stringResource(Res.string.max_tokens),
            value = config.maxTokens.toFloat(),
            valueText = config.maxTokens.toString(),
            range = MAX_TOKENS_MIN..MAX_TOKENS_MAX,
            steps = MAX_TOKENS_STEPS,
            onChange = { onChange(config.copy(maxTokens = snapTokens(it))) },
        )
        LabeledSlider(
            label = stringResource(Res.string.top_k),
            value = config.topK.toFloat(),
            valueText = config.topK.toString(),
            range = TOP_K_MIN..TOP_K_MAX,
            steps = TOP_K_STEPS,
            onChange = { onChange(config.copy(topK = it.roundToInt())) },
        )
        LabeledSlider(
            label = stringResource(Res.string.top_p),
            value = config.topP.toFloat(),
            valueText = fmt2(config.topP),
            range = TOP_P_MIN..TOP_P_MAX,
            steps = TOP_P_STEPS,
            onChange = { onChange(config.copy(topP = snap2(it))) },
        )
        LabeledSlider(
            label = stringResource(Res.string.temperature),
            value = config.temperature.toFloat(),
            valueText = fmt2(config.temperature),
            range = TEMPERATURE_MIN..TEMPERATURE_MAX,
            steps = TEMPERATURE_STEPS,
            onChange = { onChange(config.copy(temperature = snap2(it))) },
        )
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
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = LlmBackend.entries.size),
                ) { Text(entry.name) }
            }
        }
    }
}

private const val TOKEN_STEP = 256
private const val MAX_TOKENS_MIN = 256f
private const val MAX_TOKENS_MAX = 8192f
private const val MAX_TOKENS_STEPS = 30
private const val TOP_K_MIN = 1f
private const val TOP_K_MAX = 128f
private const val TOP_K_STEPS = 126
private const val TOP_P_MIN = 0.05f
private const val TOP_P_MAX = 1f
private const val TOP_P_STEPS = 18
private const val TEMPERATURE_MIN = 0f
private const val TEMPERATURE_MAX = 2f
private const val TEMPERATURE_STEPS = 39
private const val TWO_DECIMAL_PLACES = 100.0

private fun snapTokens(value: Float): Int = (value / TOKEN_STEP).roundToInt() * TOKEN_STEP

private fun snap2(value: Float): Double = (value * TWO_DECIMAL_PLACES).roundToInt() / TWO_DECIMAL_PLACES

private fun fmt2(value: Double): String = ((value * TWO_DECIMAL_PLACES).roundToInt() / TWO_DECIMAL_PLACES).toString()
