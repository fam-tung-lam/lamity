package com.phamtunglam.lamity.core.data.db.daos

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.phamtunglam.lamity.core.data.db.entities.ToolEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ToolsDao {
    @Query("SELECT * FROM tools")
    fun observeAll(): Flow<List<ToolEntity>>

    @Query("SELECT * FROM tools")
    suspend fun getAll(): List<ToolEntity>

    @Upsert
    suspend fun upsertAll(entities: List<ToolEntity>)

    @Query("DELETE FROM tools WHERE id = :id")
    suspend fun delete(id: String)
}
