package com.phamtunglam.lamity.core.data.db.entities

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * A custom model row — pure metadata. Only user-added custom models are persisted here; the built-in
 * catalog and its default inference config come from code (`ModelCatalog`), not this table. Inference
 * config is held in memory per chat, never stored per model.
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
    /** Whether the model can use tools / skills; when false the chat attaches neither. */
    @ColumnInfo(name = "supports_tools", defaultValue = "1") val supportsTools: Boolean,
    @ColumnInfo(name = "learn_more_url") val learnMoreUrl: String,
)
