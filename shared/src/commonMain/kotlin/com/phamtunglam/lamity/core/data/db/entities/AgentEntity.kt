package com.phamtunglam.lamity.core.data.db.entities

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * An agent: persona + system prompt. Its model is a one-to-one reference via [modelId] (a model can
 * back many agents); its config override, skills and tools live in related tables
 * ([AgentConfigEntity], [AgentSkillCrossRef], [AgentToolCrossRef]). [modelId] is required, and the
 * agent is cascade-deleted when its model is removed.
 */
@Entity(
    tableName = "agents",
    foreignKeys = [
        ForeignKey(
            entity = ModelEntity::class,
            parentColumns = ["id"],
            childColumns = ["model_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("model_id")],
)
data class AgentEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "system_prompt") val systemPrompt: String,
    /** Model this agent runs on. */
    @ColumnInfo(name = "model_id") val modelId: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
