package com.phamtunglam.lamity.db.entities

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [Index("conversationId")],
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String,
    val content: String,
    val thought: String,
    val toolName: String,
    val toolArgs: String,
    val toolResult: String,
    val genMillis: Long,
    val tokensPerSec: Double,
    val createdAt: Long,
)
