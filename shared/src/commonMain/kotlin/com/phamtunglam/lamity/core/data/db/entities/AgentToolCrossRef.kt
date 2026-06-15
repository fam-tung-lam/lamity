package com.phamtunglam.lamity.core.data.db.entities

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index

/** Junction row for the agent ⇄ tool many-to-many relationship (replaces `toolIdsJson`). */
@Entity(
    tableName = "agent_tools",
    primaryKeys = ["agent_id", "tool_id"],
    foreignKeys = [
        ForeignKey(
            entity = AgentEntity::class,
            parentColumns = ["id"],
            childColumns = ["agent_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ToolEntity::class,
            parentColumns = ["id"],
            childColumns = ["tool_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("tool_id")],
)
data class AgentToolCrossRef(
    @ColumnInfo(name = "agent_id") val agentId: String,
    @ColumnInfo(name = "tool_id") val toolId: String,
)
