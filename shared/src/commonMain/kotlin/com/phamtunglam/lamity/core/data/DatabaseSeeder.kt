package com.phamtunglam.lamity.core.data

import co.touchlab.kermit.Logger
import com.phamtunglam.lamity.core.data.db.daos.AgentsDao
import com.phamtunglam.lamity.core.data.db.daos.ModelsDao
import com.phamtunglam.lamity.core.data.db.daos.SkillsDao
import com.phamtunglam.lamity.core.data.db.daos.ToolsDao
import com.phamtunglam.lamity.core.data.db.entities.ToolEntity
import com.phamtunglam.lamity.core.domain.platform.epochMillis
import com.phamtunglam.lamity.feature.agents.data.toAgentConfigEntity
import com.phamtunglam.lamity.feature.agents.data.toAgentEntity
import com.phamtunglam.lamity.feature.agents.domain.AgentSeedData
import com.phamtunglam.lamity.feature.models.data.toModelEntity
import com.phamtunglam.lamity.feature.models.domain.ModelCatalog
import com.phamtunglam.lamity.feature.skills.data.toSkillEntity
import com.phamtunglam.lamity.feature.skills.domain.SkillSeedData
import com.phamtunglam.lamity.feature.tools.domain.AppTool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Populates first-launch data once, in foreign-key order: models → tools → skills → agents (whose
 * config + skill/tool links reference the rows seeded before them). Each table is seeded only when
 * empty, so user edits and deletions are never overwritten. Runs at startup on the app scope.
 */
class DatabaseSeeder(
    private val models: ModelsDao,
    private val tools: ToolsDao,
    private val skills: SkillsDao,
    private val agents: AgentsDao,
    private val builtinTools: List<AppTool>,
    scope: CoroutineScope,
) {
    private val log = Logger.withTag("DatabaseSeeder")

    init {
        scope.launch { seed() }
    }

    @Suppress("TooGenericExceptionCaught")
    suspend fun seed() {
        try {
            if (models.getAll().isEmpty()) {
                models.upsertAll(ModelCatalog.seed.map { it.toModelEntity() })
            }
            if (tools.getAll().isEmpty()) {
                tools.upsertAll(
                    builtinTools.map { ToolEntity(id = it.id, name = it.displayName, description = it.description) },
                )
            }
            val now = epochMillis()
            if (skills.getAll().isEmpty()) {
                skills.upsertAll(SkillSeedData.sampleSkills(now).map { it.toSkillEntity() })
            }
            if (agents.getAllWithRelations().isEmpty()) {
                AgentSeedData.sampleAgents(now).forEach { agent ->
                    agents.upsertGraph(
                        agent = agent.toAgentEntity(),
                        config = agent.toAgentConfigEntity(),
                        skillIds = agent.skillIds,
                        toolIds = agent.toolIds,
                    )
                }
            }
        } catch (e: Throwable) {
            log.e(e) { "database seeding failed" }
        }
    }
}
