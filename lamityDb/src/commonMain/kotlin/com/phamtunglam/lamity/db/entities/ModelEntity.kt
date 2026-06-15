package com.phamtunglam.lamity.db.entities

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Catalog or custom model row. Config columns are deliberately flat (current
 * and default values side by side) so the schema stays obvious.
 */
@Entity(tableName = "models")
data class ModelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val url: String,
    val fileName: String,
    val sizeBytes: Long,
    val requiresAuth: Boolean,
    val isCustom: Boolean,
    val supportsThinking: Boolean,
    /** Whether the model is exposed tools / skills (gates the agent-wizard steps). */
    @ColumnInfo(defaultValue = "1") val supportsTools: Boolean,
    val learnMoreUrl: String,
    val backend: String,
    val maxTokens: Int,
    val topK: Int,
    val topP: Double,
    val temperature: Double,
    val defaultBackend: String,
    val defaultMaxTokens: Int,
    val defaultTopK: Int,
    val defaultTopP: Double,
    val defaultTemperature: Double,
)
