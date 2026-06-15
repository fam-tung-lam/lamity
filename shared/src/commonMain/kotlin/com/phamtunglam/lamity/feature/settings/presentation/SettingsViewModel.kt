package com.phamtunglam.lamity.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phamtunglam.lamity.core.domain.platform.AppDirs
import com.phamtunglam.lamity.core.domain.platform.PlatformInfo
import com.phamtunglam.lamity.feature.settings.data.SettingsRepository
import com.phamtunglam.lamity.feature.settings.domain.AppSettings
import com.phamtunglam.lamity.feature.settings.domain.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val platformInfo: PlatformInfo,
    val modelsDir: String,
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    platformInfo: PlatformInfo,
    dirs: AppDirs,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> =
        settingsRepository.settings
            .map { SettingsUiState(it, platformInfo, dirs.modelsDir) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                SettingsUiState(settingsRepository.value, platformInfo, dirs.modelsDir),
            )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setWifiOnlyDownloads(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setWifiOnlyDownloads(enabled) }
    }
}
