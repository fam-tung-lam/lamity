package com.phamtunglam.lamity.core.data.db.entities

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index

/** Junction row for the agent ⇄ skill many-to-many relationship (replaces `skillIdsJson`). */
@Entity(
    tableName = "agent_skills",
    primaryKeys = ["agent_id", "skill_id"],
    foreignKeys = [
        ForeignKey(
            entity = AgentEntity::class,
            parentColumns = ["id"],
            childColumns = ["agent_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SkillEntity::class,
            parentColumns = ["id"],
            childColumns = ["skill_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("skill_id")],
)
data class AgentSkillCrossRef(
    @ColumnInfo(name = "agent_id") val agentId: String,
    @ColumnInfo(name = "skill_id") val skillId: String,
)
