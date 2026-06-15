package com.phamtunglam.lamity.core.data.db.daos

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.phamtunglam.lamity.core.data.db.entities.AgentConfigEntity
import com.phamtunglam.lamity.core.data.db.entities.AgentEntity
import com.phamtunglam.lamity.core.data.db.entities.AgentSkillCrossRef
import com.phamtunglam.lamity.core.data.db.entities.AgentToolCrossRef
import com.phamtunglam.lamity.core.data.db.relations.AgentWithRelations
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentsDao {
    @Transaction
    @Query("SELECT * FROM agents")
    fun observeAllWithRelations(): Flow<List<AgentWithRelations>>

    @Transaction
    @Query("SELECT * FROM agents")
    suspend fun getAllWithRelations(): List<AgentWithRelations>

    @Upsert
    suspend fun upsertAgent(agent: AgentEntity)

    @Upsert
    suspend fun upsertConfig(config: AgentConfigEntity)

    @Query("DELETE FROM agent_configs WHERE agent_id = :agentId")
    suspend fun clearConfig(agentId: String)

    @Upsert
    suspend fun insertSkillRefs(refs: List<AgentSkillCrossRef>)

    @Query("DELETE FROM agent_skills WHERE agent_id = :agentId")
    suspend fun clearSkillRefs(agentId: String)

    @Upsert
    suspend fun insertToolRefs(refs: List<AgentToolCrossRef>)

    @Query("DELETE FROM agent_tools WHERE agent_id = :agentId")
    suspend fun clearToolRefs(agentId: String)

    @Query("DELETE FROM agents WHERE id = :id")
    suspend fun delete(id: String)

    /**
     * Upserts an agent together with its config override and skill/tool links in a single
     * transaction. Existing config and links for the agent are replaced.
     */
    @Transaction
    suspend fun upsertGraph(
        agent: AgentEntity,
        config: AgentConfigEntity?,
        skillIds: List<String>,
        toolIds: List<String>,
    ) {
        upsertAgent(agent)
        clearConfig(agent.id)
        if (config != null) upsertConfig(config)
        clearSkillRefs(agent.id)
        if (skillIds.isNotEmpty()) insertSkillRefs(skillIds.map { AgentSkillCrossRef(agent.id, it) })
        clearToolRefs(agent.id)
        if (toolIds.isNotEmpty()) insertToolRefs(toolIds.map { AgentToolCrossRef(agent.id, it) })
    }
}
