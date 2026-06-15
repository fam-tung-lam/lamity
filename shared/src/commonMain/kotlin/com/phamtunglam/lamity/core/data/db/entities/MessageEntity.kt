package com.phamtunglam.lamity.core.data.db.entities

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.phamtunglam.lamity.llm.model.Role

/**
 * A single chat message. [role] is the lamityLlm [Role] enum (stored via a TypeConverter rather than
 * a raw string). Belongs to one conversation (many-to-one) and is cascade-deleted with it.
 */
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversation_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("conversation_id")],
)
data class MessageEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "conversation_id") val conversationId: String,
    @ColumnInfo(name = "role") val role: Role,
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "thought") val thought: String,
    @ColumnInfo(name = "tool_name") val toolName: String,
    @ColumnInfo(name = "tool_args") val toolArgs: String,
    @ColumnInfo(name = "tool_result") val toolResult: String,
    @ColumnInfo(name = "gen_millis") val genMillis: Long,
    @ColumnInfo(name = "tokens_per_sec") val tokensPerSec: Double,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
