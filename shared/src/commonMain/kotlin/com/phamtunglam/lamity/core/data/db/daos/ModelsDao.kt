package com.phamtunglam.lamity.core.data.db.daos

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.phamtunglam.lamity.core.data.db.entities.ModelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelsDao {
    @Query("SELECT * FROM models")
    fun observeAll(): Flow<List<ModelEntity>>

    @Query("SELECT * FROM models")
    suspend fun getAll(): List<ModelEntity>

    @Upsert
    suspend fun upsert(entity: ModelEntity)

    @Upsert
    suspend fun upsertAll(entities: List<ModelEntity>)

    @Query("DELETE FROM models WHERE id = :id")
    suspend fun delete(id: String)
}
