package com.phamtunglam.lamity.feature.models.data

import com.phamtunglam.lamity.core.domain.platform.newId
import com.phamtunglam.lamity.db.entities.ModelEntity
import com.phamtunglam.lamity.db.daos.ModelsDao
import com.phamtunglam.lamity.feature.models.domain.LlmBackend
import com.phamtunglam.lamity.feature.models.domain.LlmModel
import com.phamtunglam.lamity.feature.models.domain.ModelCatalog
import com.phamtunglam.lamity.feature.models.domain.ModelConfig
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

/**
 * The model catalog: seed metadata merged with what the database remembers
 * (user-tweaked configs, custom models). The database is the single source
 * of truth for everything user-mutable; only rows that diverge from the seed
 * are ever written.
 */
class ModelsRepositoryImpl(
    private val dao: ModelsDao,
    scope: CoroutineScope,
) : ModelsRepository {

    private val log = Logger.withTag("ModelsRepository")

    private val loaded = CompletableDeferred<Unit>()

    override val models: StateFlow<List<LlmModel>> = dao.observeAll()
        .map { rows -> mergeWithSeed(rows.map { it.toDomain() }) }
        .catch { e ->
            log.e(e) { "failed to observe models" }
            emit(mergeWithSeed(emptyList()))
        }
        .onEach { if (!loaded.isCompleted) loaded.complete(Unit) }
        .stateIn(scope, SharingStarted.Eagerly, mergeWithSeed(emptyList()))

    override suspend fun awaitLoaded() = loaded.await()

    override fun byId(id: String?): LlmModel? =
        id?.let { i -> models.value.firstOrNull { it.id == i } }

    override suspend fun updateConfig(modelId: String, config: ModelConfig) {
        val model = byId(modelId) ?: return
        runCatching { dao.upsert(model.copy(config = config).toEntity()) }
            .onFailure { log.e(it) { "failed to persist config for $modelId" } }
    }

    override suspend fun addCustomModel(name: String, url: String, requiresAuth: Boolean): LlmModel {
        val cleanUrl = url.trim()
        val fileName = cleanUrl.substringAfterLast('/').substringBefore('?')
            .ifBlank { "custom-${newId().take(8)}.litertlm" }
        val model = LlmModel(
            id = "custom-${newId().take(8)}",
            name = name.trim().ifBlank { fileName },
            description = "Custom model added by URL.",
            url = cleanUrl,
            fileName = fileName,
            isCustom = true,
            requiresAuth = requiresAuth,
        )
        runCatching { dao.upsert(model.toEntity()) }
            .onFailure { log.e(it) { "failed to persist custom model" } }
        return model
    }

    override suspend fun removeCustomModel(modelId: String) {
        if (byId(modelId)?.isCustom != true) return
        runCatching { dao.delete(modelId) }
            .onFailure { log.e(it) { "failed to delete model $modelId" } }
    }

    /** Seed catalog metadata always wins, but user-tweaked config and custom models survive. */
    private fun mergeWithSeed(stored: List<LlmModel>): List<LlmModel> {
        val storedById = stored.associateBy { it.id }
        val seeded = ModelCatalog.seed.map { seed ->
            storedById[seed.id]?.let { seed.copy(config = it.config) } ?: seed
        }
        return seeded + stored.filter { it.isCustom }
    }
}

internal fun ModelEntity.toDomain() = LlmModel(
    id = id,
    name = name,
    description = description,
    url = url,
    fileName = fileName,
    sizeBytes = sizeBytes,
    requiresAuth = requiresAuth,
    isCustom = isCustom,
    supportsThinking = supportsThinking,
    learnMoreUrl = learnMoreUrl,
    config = ModelConfig(backendOf(backend), maxTokens, topK, topP, temperature),
    defaultConfig = ModelConfig(
        backendOf(defaultBackend), defaultMaxTokens, defaultTopK, defaultTopP, defaultTemperature,
    ),
)

internal fun LlmModel.toEntity() = ModelEntity(
    id = id,
    name = name,
    description = description,
    url = url,
    fileName = fileName,
    sizeBytes = sizeBytes,
    requiresAuth = requiresAuth,
    isCustom = isCustom,
    supportsThinking = supportsThinking,
    learnMoreUrl = learnMoreUrl,
    backend = config.backend.name,
    maxTokens = config.maxTokens,
    topK = config.topK,
    topP = config.topP,
    temperature = config.temperature,
    defaultBackend = defaultConfig.backend.name,
    defaultMaxTokens = defaultConfig.maxTokens,
    defaultTopK = defaultConfig.topK,
    defaultTopP = defaultConfig.topP,
    defaultTemperature = defaultConfig.temperature,
)

private fun backendOf(name: String): LlmBackend =
    runCatching { LlmBackend.valueOf(name) }.getOrDefault(LlmBackend.GPU)
