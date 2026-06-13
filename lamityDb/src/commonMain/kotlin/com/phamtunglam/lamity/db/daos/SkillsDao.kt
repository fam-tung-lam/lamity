package com.phamtunglam.lamity.db.daos

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.phamtunglam.lamity.db.entities.SkillEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SkillsDao {
    @Query("SELECT * FROM skills")
    fun observeAll(): Flow<List<SkillEntity>>

    @Query("SELECT * FROM skills")
    suspend fun getAll(): List<SkillEntity>

    @Upsert
    suspend fun upsert(entity: SkillEntity)

    @Upsert
    suspend fun upsertAll(entities: List<SkillEntity>)

    @Query("DELETE FROM skills WHERE id = :id")
    suspend fun delete(id: String)
}
