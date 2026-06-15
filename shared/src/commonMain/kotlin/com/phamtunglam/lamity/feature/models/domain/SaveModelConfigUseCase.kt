package com.phamtunglam.lamity.feature.models.domain

import com.phamtunglam.lamity.feature.models.data.ModelsRepository
import kotlin.math.roundToInt

/**
 * Persists a model configuration, snapping the raw slider values to their
 * valid grid: maxTokens to multiples of 256, topK to whole numbers, topP and
 * temperature to two decimals.
 */
class SaveModelConfigUseCase(private val models: ModelsRepository) {
    suspend operator fun invoke(
        modelId: String,
        backend: LlmBackend,
        maxTokens: Float,
        topK: Float,
        topP: Float,
        temperature: Float,
    ) {
        val model = models.byId(modelId) ?: return
        models.updateConfig(
            modelId,
            model.config.copy(
                backend = backend,
                maxTokens = (maxTokens / 256f).roundToInt() * 256,
                topK = topK.roundToInt(),
                topP = (topP * 100).roundToInt() / 100.0,
                temperature = (temperature * 100).roundToInt() / 100.0,
            ),
        )
    }
}
