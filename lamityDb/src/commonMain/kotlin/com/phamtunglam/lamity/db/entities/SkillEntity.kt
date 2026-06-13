package com.phamtunglam.lamity.db.entities

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "skills")
data class SkillEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val instructions: String,
    val enabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
