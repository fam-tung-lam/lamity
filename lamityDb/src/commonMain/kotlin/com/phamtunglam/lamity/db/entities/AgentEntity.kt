package com.phamtunglam.lamity.db.entities

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "agents")
data class AgentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val systemPrompt: String,
    /** JSON array of tool ids. */
    val toolIdsJson: String,
    /** JSON array of skill ids. */
    val skillIdsJson: String,
    val createdAt: Long,
    val updatedAt: Long,
)
