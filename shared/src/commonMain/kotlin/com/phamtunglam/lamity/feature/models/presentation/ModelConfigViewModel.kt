package com.phamtunglam.lamity.feature.models.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phamtunglam.lamity.feature.models.data.ModelsRepository
import com.phamtunglam.lamity.feature.models.domain.LlmModel
import com.phamtunglam.lamity.feature.models.domain.ModelConfig
import com.phamtunglam.lamity.feature.models.domain.SaveModelConfigUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ModelConfigUiState(
    val isLoading: Boolean = true,
    val model: LlmModel? = null,
    val config: ModelConfig = ModelConfig(),
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
            ModelConfigUiState(isLoading = false, model = it, config = it.config)
        } ?: ModelConfigUiState(isLoading = false)

    fun setConfig(config: ModelConfig) = _uiState.update { it.copy(config = config) }

    fun resetToDefaults() {
        val model = _uiState.value.model ?: return
        _uiState.update { it.copy(config = model.defaultConfig) }
    }

    fun save() {
        val s = _uiState.value
        if (s.model == null) return
        viewModelScope.launch {
            saveModelConfig(
                modelId = modelId,
                backend = s.config.backend,
                maxTokens = s.config.maxTokens.toFloat(),
                topK = s.config.topK.toFloat(),
                topP = s.config.topP.toFloat(),
                temperature = s.config.temperature.toFloat(),
            )
            _uiState.update { it.copy(saved = true) }
        }
    }
}
