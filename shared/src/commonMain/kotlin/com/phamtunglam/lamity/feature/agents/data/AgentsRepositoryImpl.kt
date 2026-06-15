package com.phamtunglam.lamity.feature.agents.data

import co.touchlab.kermit.Logger
import com.phamtunglam.lamity.core.data.db.daos.AgentsDao
import com.phamtunglam.lamity.core.data.db.entities.AgentEntity
import com.phamtunglam.lamity.core.data.db.mappers.toAgentConfigEntity
import com.phamtunglam.lamity.core.data.db.mappers.toModelConfig
import com.phamtunglam.lamity.core.data.db.relations.AgentWithRelations
import com.phamtunglam.lamity.core.domain.platform.epochMillis
import com.phamtunglam.lamity.core.domain.platform.newId
import com.phamtunglam.lamity.feature.agents.domain.Agent
import com.phamtunglam.lamity.feature.models.domain.ModelConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

/**
 * Agents live in Room; the database is the single source of truth (seeded by DatabaseSeeder). An
 * agent's model, config override, skills and tools are persisted relationally and read back as a
 * single [AgentWithRelations] graph.
 */
class AgentsRepositoryImpl(private val dao: AgentsDao, scope: CoroutineScope) : AgentsRepository {
    private val log = Logger.withTag("AgentsRepository")

    private val loaded = CompletableDeferred<Unit>()

    override val agents: StateFlow<List<Agent>> =
        dao
            .observeAllWithRelations()
            .map { rows -> rows.map { it.toDomain() } }
            .catch { e ->
                log.e(e) { "failed to observe agents" }
                emit(emptyList())
            }.onEach { if (!loaded.isCompleted) loaded.complete(Unit) }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override suspend fun awaitLoaded() = loaded.await()

    override fun byId(id: String?): Agent? = id?.let { i -> agents.value.firstOrNull { it.id == i } }

    override suspend fun upsert(
        id: String?,
        name: String,
        description: String,
        systemPrompt: String,
        toolIds: List<String>,
        skillIds: List<String>,
        modelId: String,
        modelConfig: ModelConfig?,
    ): Agent {
        val now = epochMillis()
        val existing = byId(id)
        val agent =
            Agent(
                id = existing?.id ?: "agent-${newId().take(8)}",
                name = name.trim(),
                description = description.trim(),
                systemPrompt = systemPrompt.trim(),
                toolIds = toolIds.distinct(),
                skillIds = skillIds.distinct(),
                modelId = modelId,
                modelConfig = modelConfig,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            )
        runCatching {
            dao.upsertGraph(
                agent = agent.toAgentEntity(),
                config = agent.toAgentConfigEntity(),
                skillIds = agent.skillIds,
                toolIds = agent.toolIds,
            )
        }.onFailure { log.e(it) { "failed to persist agent ${agent.id}" } }
        return agent
    }

    override suspend fun delete(agentId: String) {
        // Config and skill/tool junction rows cascade-delete with the agent.
        runCatching { dao.delete(agentId) }
            .onFailure { log.e(it) { "failed to delete agent $agentId" } }
    }
}

internal fun AgentWithRelations.toDomain() =
    Agent(
        id = agent.id,
        name = agent.name,
        description = agent.description,
        systemPrompt = agent.systemPrompt,
        toolIds = tools.map { it.id },
        skillIds = skills.map { it.id },
        modelId = agent.modelId,
        modelConfig = config?.toModelConfig(),
        createdAt = agent.createdAt,
        updatedAt = agent.updatedAt,
    )

internal fun Agent.toAgentEntity() =
    AgentEntity(
        id = id,
        name = name,
        description = description,
        systemPrompt = systemPrompt,
        modelId = modelId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun Agent.toAgentConfigEntity() = modelConfig?.toAgentConfigEntity(id)
