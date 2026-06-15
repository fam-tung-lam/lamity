package com.phamtunglam.lamity.core.data.db.entities

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.PrimaryKey
import com.phamtunglam.lamity.feature.models.domain.LlmBackend

/**
 * One-to-one inference-config override for an agent. The sampling fields are stored as flat columns
 * (no shared embeddable); [backend] is persisted through the database converters (LlmBackend ⇄
 * String). Keyed by [agentId] and cascade-deleted with its agent.
 */
@Entity(
    tableName = "agent_configs",
    foreignKeys = [
        ForeignKey(
            entity = AgentEntity::class,
            parentColumns = ["id"],
            childColumns = ["agent_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class AgentConfigEntity(
    @PrimaryKey @ColumnInfo(name = "agent_id") val agentId: String,
    @ColumnInfo(name = "backend") val backend: LlmBackend,
    @ColumnInfo(name = "max_tokens") val maxTokens: Int,
    @ColumnInfo(name = "top_k") val topK: Int,
    @ColumnInfo(name = "top_p") val topP: Double,
    @ColumnInfo(name = "temperature") val temperature: Double,
)
