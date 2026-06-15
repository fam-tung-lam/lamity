package com.phamtunglam.lamity.feature.models.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phamtunglam.lamity.feature.models.data.ModelsRepository
import com.phamtunglam.lamity.feature.models.domain.LlmBackend
import com.phamtunglam.lamity.feature.models.domain.LlmModel
import com.phamtunglam.lamity.feature.models.domain.SaveModelConfigUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ModelConfigUiState(
    val isLoading: Boolean = true,
    val model: LlmModel? = null,
    val backend: LlmBackend = LlmBackend.GPU,
    val maxTokens: Float = 2048f,
    val topK: Float = 40f,
    val topP: Float = 0.95f,
    val temperature: Float = 0.8f,
    /** Set once the config has been persisted; the UI navigates back on it. */
    val saved: Boolean = false,
)

class ModelConfigViewModel(
    private val modelId: String,
    private val models: ModelsRepository,
    private val saveModelConfig: SaveModelConfigUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ModelConfigUiState())
    val uiState: StateFlow<ModelConfigUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            models.awaitLoaded()
            _uiState.value = stateFor(models.byId(modelId))
        }
    }

    private fun stateFor(model: LlmModel?): ModelConfigUiState =
        model?.let {
            ModelConfigUiState(
                isLoading = false,
                model = it,
                backend = it.config.backend,
                maxTokens = it.config.maxTokens.toFloat(),
                topK = it.config.topK.toFloat(),
                topP = it.config.topP.toFloat(),
                temperature = it.config.temperature.toFloat(),
            )
        } ?: ModelConfigUiState(isLoading = false)

    fun setBackend(backend: LlmBackend) = _uiState.update { it.copy(backend = backend) }

    fun setMaxTokens(value: Float) = _uiState.update { it.copy(maxTokens = value) }

    fun setTopK(value: Float) = _uiState.update { it.copy(topK = value) }

    fun setTopP(value: Float) = _uiState.update { it.copy(topP = value) }

    fun setTemperature(value: Float) = _uiState.update { it.copy(temperature = value) }

    fun resetToDefaults() {
        val model = _uiState.value.model ?: return
        _uiState.update {
            it.copy(
                backend = model.defaultConfig.backend,
                maxTokens = model.defaultConfig.maxTokens.toFloat(),
                topK = model.defaultConfig.topK.toFloat(),
                topP = model.defaultConfig.topP.toFloat(),
                temperature = model.defaultConfig.temperature.toFloat(),
            )
        }
    }

    fun save() {
        val s = _uiState.value
        if (s.model == null) return
        viewModelScope.launch {
            saveModelConfig(
                modelId = modelId,
                backend = s.backend,
                maxTokens = s.maxTokens,
                topK = s.topK,
                topP = s.topP,
                temperature = s.temperature,
            )
            _uiState.update { it.copy(saved = true) }
        }
    }
}
