package com.phamtunglam.lamity.core.data.db.entities

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * A conversation is a pure message thread: a title and timestamps. It deliberately holds no
 * model/agent/tool/skill references and no system-prompt override — what a chat runs with (model,
 * agent, an agent-less system prompt) is the live app selection (see the chat session), never
 * persisted per conversation.
 */
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
