package com.phamtunglam.lamity.db.daos

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.phamtunglam.lamity.db.entities.AgentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentsDao {
    @Query("SELECT * FROM agents")
    fun observeAll(): Flow<List<AgentEntity>>

    @Query("SELECT * FROM agents")
    suspend fun getAll(): List<AgentEntity>

    @Upsert
    suspend fun upsert(entity: AgentEntity)

    @Upsert
    suspend fun upsertAll(entities: List<AgentEntity>)

    @Query("DELETE FROM agents WHERE id = :id")
    suspend fun delete(id: String)
}
