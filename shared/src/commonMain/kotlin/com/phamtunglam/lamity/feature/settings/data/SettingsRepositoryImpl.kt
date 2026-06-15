package com.phamtunglam.lamity.feature.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import co.touchlab.kermit.Logger
import com.phamtunglam.lamity.feature.settings.domain.AppSettings
import com.phamtunglam.lamity.feature.settings.domain.ThemeMode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

/**
 * Settings live in the app-wide preferences DataStore, the single source of
 * truth. Reads observe the stored preferences reactively; writes go through an
 * atomic read-modify-write ([DataStore.edit] is serialized) so rapid successive
 * updates never overwrite each other.
 */
class SettingsRepositoryImpl(private val dataStore: DataStore<Preferences>, scope: CoroutineScope) :
    SettingsRepository {
    private val log = Logger.withTag("SettingsRepository")

    private val loaded = CompletableDeferred<Unit>()

    override val settings: StateFlow<AppSettings> =
        dataStore.data
            .map { preferences -> preferences.toAppSettings() }
            .catch { e ->
                log.e(e) { "failed to observe settings" }
                emit(AppSettings())
            }.onEach { if (!loaded.isCompleted) loaded.complete(Unit) }
            .stateIn(scope, SharingStarted.Eagerly, AppSettings())

    override suspend fun awaitLoaded() = loaded.await()

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        runCatching {
            dataStore.edit { preferences ->
                preferences.writeAppSettings(transform(preferences.toAppSettings()))
            }
        }.onFailure { log.e(it) { "failed to persist settings" } }
    }
}

private val ThemeModeKey = stringPreferencesKey("theme_mode")
private val LastModelIdKey = stringPreferencesKey("last_model_id")
private val LastAgentIdKey = stringPreferencesKey("last_agent_id")
private val WifiOnlyDownloadsKey = booleanPreferencesKey("wifi_only_downloads")

private fun Preferences.toAppSettings() =
    AppSettings(
        themeMode =
            this[ThemeModeKey]
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
        lastModelId = this[LastModelIdKey],
        lastAgentId = this[LastAgentIdKey],
        wifiOnlyDownloads = this[WifiOnlyDownloadsKey] ?: false,
    )

private fun MutablePreferences.writeAppSettings(settings: AppSettings) {
    this[ThemeModeKey] = settings.themeMode.name
    settings.lastModelId.let { if (it == null) remove(LastModelIdKey) else this[LastModelIdKey] = it }
    settings.lastAgentId.let { if (it == null) remove(LastAgentIdKey) else this[LastAgentIdKey] = it }
    this[WifiOnlyDownloadsKey] = settings.wifiOnlyDownloads
}
