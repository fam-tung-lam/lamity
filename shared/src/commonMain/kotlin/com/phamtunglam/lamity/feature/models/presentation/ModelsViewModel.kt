package com.phamtunglam.lamity.feature.models.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phamtunglam.lamity.feature.models.data.ModelsRepository
import com.phamtunglam.lamity.feature.models.domain.CancelModelDownloadUseCase
import com.phamtunglam.lamity.feature.models.domain.DeleteModelFileUseCase
import com.phamtunglam.lamity.feature.models.domain.LlmModel
import com.phamtunglam.lamity.feature.models.domain.ModelWithStatus
import com.phamtunglam.lamity.feature.models.domain.ObserveModelsWithStatusUseCase
import com.phamtunglam.lamity.feature.models.domain.PauseModelDownloadUseCase
import com.phamtunglam.lamity.feature.models.domain.RemoveCustomModelUseCase
import com.phamtunglam.lamity.feature.models.domain.ResumeModelDownloadUseCase
import com.phamtunglam.lamity.feature.models.domain.SelectModelForChatUseCase
import com.phamtunglam.lamity.feature.models.domain.StartModelDownloadUseCase
import com.phamtunglam.lamity.feature.settings.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ModelsUiState(
    val rows: List<ModelWithStatus> = emptyList(),
    /** The model currently selected for chatting (shows a "Selected" badge). */
    val selectedModelId: String? = null,
)

class ModelsViewModel(
    observeModelsWithStatus: ObserveModelsWithStatusUseCase,
    settings: SettingsRepository,
    private val removeCustomModelUseCase: RemoveCustomModelUseCase,
    private val models: ModelsRepository,
    private val startDownloadUseCase: StartModelDownloadUseCase,
    private val pauseDownloadUseCase: PauseModelDownloadUseCase,
    private val resumeDownloadUseCase: ResumeModelDownloadUseCase,
    private val cancelDownloadUseCase: CancelModelDownloadUseCase,
    private val deleteModelFileUseCase: DeleteModelFileUseCase,
    private val selectModelForChatUseCase: SelectModelForChatUseCase,
) : ViewModel() {
    /** Bumped after local file changes (delete) so the status flow re-checks on-disk state. */
    private val filesChanged = MutableStateFlow(0)

    val uiState: StateFlow<ModelsUiState> =
        combine(
            observeModelsWithStatus(filesChanged.map { }),
            settings.settings,
        ) { rows, appSettings ->
            ModelsUiState(rows = rows, selectedModelId = appSettings.lastModelId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ModelsUiState())

    fun download(model: LlmModel) {
        viewModelScope.launch { startDownloadUseCase(model) }
    }

    fun pauseDownload(model: LlmModel) {
        viewModelScope.launch { pauseDownloadUseCase(model) }
    }

    fun resumeDownload(model: LlmModel) {
        viewModelScope.launch { resumeDownloadUseCase(model) }
    }

    fun cancelDownload(model: LlmModel) {
        viewModelScope.launch { cancelDownloadUseCase(model) }
    }

    fun dismissError(model: LlmModel) = cancelDownload(model)

    fun deleteFile(model: LlmModel) {
        deleteModelFileUseCase(model)
        filesChanged.update { it + 1 }
    }

    fun addCustomModel(name: String, url: String, requiresAuth: Boolean) {
        viewModelScope.launch { models.addCustomModel(name, url, requiresAuth) }
    }

    fun removeCustomModel(model: LlmModel) {
        viewModelScope.launch {
            removeCustomModelUseCase(model)
            filesChanged.update { it + 1 }
        }
    }

    /** Selects [model] as the chat model; the caller navigates back to where it opened the screen. */
    fun selectForChat(model: LlmModel) {
        viewModelScope.launch { selectModelForChatUseCase(model) }
    }
}
