package com.phamtunglam.lamity.core.data.db.entities

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Catalog or custom model row — pure metadata. Inference config is no longer stored per model: it is
 * configured per agent ([AgentConfigEntity]) or held in memory for an agent-less chat. Catalog
 * defaults come from the seed (`ModelCatalog`), not this table.
 */
@Entity(tableName = "models")
data class ModelEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    @ColumnInfo(name = "requires_auth") val requiresAuth: Boolean,
    @ColumnInfo(name = "is_custom") val isCustom: Boolean,
    @ColumnInfo(name = "supports_thinking") val supportsThinking: Boolean,
    /** Whether the model is exposed tools / skills (gates the agent-wizard steps). */
    @ColumnInfo(name = "supports_tools", defaultValue = "1") val supportsTools: Boolean,
    @ColumnInfo(name = "learn_more_url") val learnMoreUrl: String,
)
