package com.phamtunglam.lamity.feature.models.data

import co.touchlab.kermit.Logger
import com.phamtunglam.lamity.core.data.db.daos.ModelsDao
import com.phamtunglam.lamity.core.data.db.entities.ModelEntity
import com.phamtunglam.lamity.core.domain.platform.newId
import com.phamtunglam.lamity.feature.models.domain.LlmModel
import com.phamtunglam.lamity.feature.models.domain.ModelCatalog
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

/**
 * The model catalog: built-in seed metadata plus any custom models the user added (which live in the
 * `models` table). Models hold no persisted inference config — catalog defaults come from the seed,
 * and per-agent / agent-less config lives elsewhere. The full catalog is also materialized into the
 * `models` table at first launch (see DatabaseSeeder) so agents can reference models relationally.
 */
class ModelsRepositoryImpl(private val dao: ModelsDao, scope: CoroutineScope) : ModelsRepository {
    private val log = Logger.withTag("ModelsRepository")

    private val loaded = CompletableDeferred<Unit>()

    override val models: StateFlow<List<LlmModel>> =
        dao
            .observeAll()
            .map { rows -> mergeWithSeed(rows.map { it.toDomain() }) }
            .catch { e ->
                log.e(e) { "failed to observe models" }
                emit(mergeWithSeed(emptyList()))
            }.onEach { if (!loaded.isCompleted) loaded.complete(Unit) }
            .stateIn(scope, SharingStarted.Eagerly, mergeWithSeed(emptyList()))

    override suspend fun awaitLoaded() = loaded.await()

    override fun byId(id: String?): LlmModel? = id?.let { i -> models.value.firstOrNull { it.id == i } }

    override suspend fun addCustomModel(name: String, url: String, requiresAuth: Boolean): LlmModel {
        val cleanUrl = url.trim()
        val fileName =
            cleanUrl
                .substringAfterLast('/')
                .substringBefore('?')
                .ifBlank { "custom-${newId().take(8)}.litertlm" }
        val model =
            LlmModel(
                id = "custom-${newId().take(8)}",
                name = name.trim().ifBlank { fileName },
                description = "Custom model added by URL.",
                url = cleanUrl,
                fileName = fileName,
                isCustom = true,
                requiresAuth = requiresAuth,
            )
        runCatching { dao.upsert(model.toModelEntity()) }
            .onFailure { log.e(it) { "failed to persist custom model" } }
        return model
    }

    override suspend fun removeCustomModel(modelId: String) {
        if (byId(modelId)?.isCustom != true) return
        runCatching { dao.delete(modelId) }
            .onFailure { log.e(it) { "failed to delete model $modelId" } }
    }

    /**
     * The built-in catalog (with its seed-defined default configs) plus any custom models the user
     * persisted. Stored catalog rows are pure metadata, so the seed is authoritative for them.
     */
    private fun mergeWithSeed(stored: List<LlmModel>): List<LlmModel> =
        ModelCatalog.seed + stored.filter { it.isCustom }
}

internal fun ModelEntity.toDomain() =
    LlmModel(
        id = id,
        name = name,
        description = description,
        url = url,
        fileName = fileName,
        sizeBytes = sizeBytes,
        requiresAuth = requiresAuth,
        isCustom = isCustom,
        supportsThinking = supportsThinking,
        supportsTools = supportsTools,
        learnMoreUrl = learnMoreUrl,
    )

internal fun LlmModel.toModelEntity() =
    ModelEntity(
        id = id,
        name = name,
        description = description,
        url = url,
        fileName = fileName,
        sizeBytes = sizeBytes,
        requiresAuth = requiresAuth,
        isCustom = isCustom,
        supportsThinking = supportsThinking,
        supportsTools = supportsTools,
        learnMoreUrl = learnMoreUrl,
    )
