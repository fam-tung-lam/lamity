package com.phamtunglam.lamity.core.data.db.entities

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * A skill: named instructions a model can pull in on demand. A skill is inactive until an agent
 * links it (see [AgentSkillCrossRef]); there is no global enabled flag.
 */
@Entity(tableName = "skills")
data class SkillEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "instructions") val instructions: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
