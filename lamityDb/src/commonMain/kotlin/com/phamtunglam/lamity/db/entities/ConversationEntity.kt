package com.phamtunglam.lamity.db.entities

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val agentId: String?,
    val modelId: String,
    /** JSON array of tool ids for an agent-less customized chat; null = defaults. */
    val customToolIdsJson: String?,
    /** JSON array of skill ids for an agent-less customized chat; null = none. */
    val customSkillIdsJson: String?,
    /** Per-chat system prompt for an agent-less customized chat; null = none. */
    val customSystemPrompt: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
