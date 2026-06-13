package com.phamtunglam.lamity.feature.models.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phamtunglam.lamity.feature.chat.domain.ChatSessionManager
import com.phamtunglam.lamity.feature.models.data.ModelDownloadManager
import com.phamtunglam.lamity.feature.models.data.ModelsRepository
import com.phamtunglam.lamity.feature.models.domain.LlmModel
import com.phamtunglam.lamity.feature.models.domain.ModelWithStatus
import com.phamtunglam.lamity.feature.models.domain.ObserveModelsWithStatusUseCase
import com.phamtunglam.lamity.feature.models.domain.RemoveCustomModelUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ModelsUiState(
    val rows: List<ModelWithStatus> = emptyList(),
)

class ModelsViewModel(
    observeModelsWithStatus: ObserveModelsWithStatusUseCase,
    private val removeCustomModelUseCase: RemoveCustomModelUseCase,
    private val models: ModelsRepository,
    private val downloads: ModelDownloadManager,
    private val chat: ChatSessionManager,
) : ViewModel() {

    val uiState: StateFlow<ModelsUiState> = observeModelsWithStatus()
        .map { ModelsUiState(rows = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ModelsUiState())

    fun download(model: LlmModel) = downloads.start(model)
    fun pauseDownload(model: LlmModel) = downloads.pause(model)
    fun resumeDownload(model: LlmModel) = downloads.resume(model)
    fun cancelDownload(model: LlmModel) = downloads.cancel(model)
    fun dismissError(model: LlmModel) = downloads.dismissError(model)
    fun deleteFile(model: LlmModel) = downloads.deleteFile(model)

    fun addCustomModel(name: String, url: String, requiresAuth: Boolean) {
        viewModelScope.launch { models.addCustomModel(name, url, requiresAuth) }
    }

    fun removeCustomModel(model: LlmModel) {
        viewModelScope.launch { removeCustomModelUseCase(model) }
    }

    /** Selects [model] for chatting; the caller navigates to the chat tab. */
    fun selectForChat(model: LlmModel) = chat.selectModel(model.id)
}
