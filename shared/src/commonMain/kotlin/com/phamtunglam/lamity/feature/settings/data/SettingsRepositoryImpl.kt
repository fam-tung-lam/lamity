package com.phamtunglam.lamity.feature.settings.data

import com.phamtunglam.lamity.db.daos.SettingsDao
import com.phamtunglam.lamity.db.entities.SettingsEntity
import com.phamtunglam.lamity.feature.settings.domain.AppSettings
import com.phamtunglam.lamity.feature.settings.domain.ThemeMode
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Settings live in one Room row (id = 0); the database is the single source
 * of truth. Reads observe the row reactively, writes go through the DAO and
 * surface back via the flow.
 */
class SettingsRepositoryImpl(
    private val dao: SettingsDao,
    scope: CoroutineScope,
) : SettingsRepository {

    private val log = Logger.withTag("SettingsRepository")

    private val loaded = CompletableDeferred<Unit>()
    private val writeMutex = Mutex()

    override val settings: StateFlow<AppSettings> = dao.observe()
        .map { entity -> entity?.toDomain() ?: AppSettings() }
        .catch { e ->
            log.e(e) { "failed to observe settings" }
            emit(AppSettings())
        }
        .onEach { if (!loaded.isCompleted) loaded.complete(Unit) }
        .stateIn(scope, SharingStarted.Eagerly, AppSettings())

    override suspend fun awaitLoaded() = loaded.await()

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        // Serialized read-modify-write against the database (not the cached
        // flow value) so rapid successive updates never overwrite each other.
        writeMutex.withLock {
            runCatching {
                val current = dao.get()?.toDomain() ?: AppSettings()
                dao.upsert(transform(current).toEntity())
            }.onFailure { log.e(it) { "failed to persist settings" } }
        }
    }
}

private val toolMapSerializer = MapSerializer(String.serializer(), Boolean.serializer())
private val json = Json { ignoreUnknownKeys = true }

private fun SettingsEntity.toDomain() = AppSettings(
    themeMode = runCatching { ThemeMode.valueOf(themeMode) }.getOrDefault(ThemeMode.SYSTEM),
    language = language,
    toolEnabled = runCatching {
        json.decodeFromString(toolMapSerializer, toolEnabledJson)
    }.getOrDefault(emptyMap()),
    lastModelId = lastModelId,
    lastAgentId = lastAgentId,
    wifiOnlyDownloads = wifiOnlyDownloads,
)

private fun AppSettings.toEntity() = SettingsEntity(
    id = 0,
    themeMode = themeMode.name,
    language = language,
    toolEnabledJson = json.encodeToString(toolMapSerializer, toolEnabled),
    lastModelId = lastModelId,
    lastAgentId = lastAgentId,
    wifiOnlyDownloads = wifiOnlyDownloads,
)
