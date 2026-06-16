package com.phamtunglam.lamity.feature.settings.data

import com.phamtunglam.lamity.feature.settings.domain.AppSettings
import com.phamtunglam.lamity.feature.settings.domain.ThemeMode
import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val settings: StateFlow<AppSettings>
    val value: AppSettings get() = settings.value

    /** Completes once persisted settings have been loaded into [settings]. */
    suspend fun awaitLoaded()

    suspend fun update(transform: (AppSettings) -> AppSettings)

    suspend fun setThemeMode(mode: ThemeMode) = update { it.copy(themeMode = mode) }

    suspend fun setWifiOnlyDownloads(enabled: Boolean) = update { it.copy(wifiOnlyDownloads = enabled) }

    suspend fun setLastModelId(modelId: String?) = update { it.copy(lastModelId = modelId) }
}
