package com.phamtunglam.lamity.db.entities

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val agentId: String?,
    val modelId: String,
    val createdAt: Long,
    val updatedAt: Long,
)
