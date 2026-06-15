package com.phamtunglam.lamity.feature.studio.data

import co.touchlab.kermit.Logger
import com.phamtunglam.lamity.core.domain.platform.epochMillis
import com.phamtunglam.lamity.core.domain.platform.newId
import com.phamtunglam.lamity.db.daos.AgentsDao
import com.phamtunglam.lamity.db.entities.AgentEntity
import com.phamtunglam.lamity.feature.studio.domain.Agent
import com.phamtunglam.lamity.feature.studio.domain.StudioSeedData
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** Agents live in Room; the database is the single source of truth. */
class AgentsRepositoryImpl(private val dao: AgentsDao, scope: CoroutineScope) : AgentsRepository {
    private val log = Logger.withTag("AgentsRepository")

    private val loaded = CompletableDeferred<Unit>()

    override val agents: StateFlow<List<Agent>> =
        dao
            .observeAll()
            .map { rows -> rows.map { it.toDomain() } }
            .catch { e ->
                log.e(e) { "failed to observe agents" }
                emit(emptyList())
            }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    init {
        scope.launch {
            val seeded =
                runCatching {
                    if (dao.getAll().isEmpty()) {
                        dao.upsertAll(StudioSeedData.sampleAgents(epochMillis()).map { it.toEntity() })
                    }
                    true
                }.onFailure { log.e(it) { "failed to seed agents" } }.getOrDefault(false)
            // Loaded once the flow reflects the stored (or just-seeded) agents.
            if (seeded) agents.first { it.isNotEmpty() }
            loaded.complete(Unit)
        }
    }

    override suspend fun awaitLoaded() = loaded.await()

    override fun byId(id: String?): Agent? = id?.let { i -> agents.value.firstOrNull { it.id == i } }

    override suspend fun upsert(
        id: String?,
        name: String,
        description: String,
        systemPrompt: String,
        toolIds: List<String>,
        skillIds: List<String>,
    ): Agent {
        val now = epochMillis()
        val existing = byId(id)
        val agent =
            (existing ?: Agent(id = "agent-${newId().take(8)}", name = "", createdAt = now))
                .copy(
                    name = name.trim(),
                    description = description.trim(),
                    systemPrompt = systemPrompt.trim(),
                    toolIds = toolIds.distinct(),
                    skillIds = skillIds.distinct(),
                    updatedAt = now,
                )
        runCatching { dao.upsert(agent.toEntity()) }
            .onFailure { log.e(it) { "failed to persist agent ${agent.id}" } }
        return agent
    }

    override suspend fun delete(agentId: String) {
        runCatching { dao.delete(agentId) }
            .onFailure { log.e(it) { "failed to delete agent $agentId" } }
    }

    override suspend fun detachSkillEverywhere(skillId: String) {
        runCatching {
            val affected =
                dao
                    .getAll()
                    .map { it.toDomain() }
                    .filter { skillId in it.skillIds }
                    .map { it.copy(skillIds = it.skillIds - skillId).toEntity() }
            if (affected.isNotEmpty()) dao.upsertAll(affected)
        }.onFailure { log.e(it) { "failed to detach skill $skillId" } }
    }
}

private val stringListSerializer = ListSerializer(String.serializer())
private val json = Json { ignoreUnknownKeys = true }

internal fun decodeStringList(text: String): List<String> =
    runCatching { json.decodeFromString(stringListSerializer, text) }.getOrDefault(emptyList())

internal fun encodeStringList(values: List<String>): String = json.encodeToString(stringListSerializer, values)

private fun AgentEntity.toDomain() =
    Agent(
        id = id,
        name = name,
        description = description,
        systemPrompt = systemPrompt,
        toolIds = decodeStringList(toolIdsJson),
        skillIds = decodeStringList(skillIdsJson),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun Agent.toEntity() =
    AgentEntity(
        id = id,
        name = name,
        description = description,
        systemPrompt = systemPrompt,
        toolIdsJson = encodeStringList(toolIds),
        skillIdsJson = encodeStringList(skillIds),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
