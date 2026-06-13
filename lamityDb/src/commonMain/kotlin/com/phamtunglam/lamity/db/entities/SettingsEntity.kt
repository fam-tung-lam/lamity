package com.phamtunglam.lamity.db.entities

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** Singleton app settings row (id = 0). */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Long = 0,
    val themeMode: String,
    val language: String,
    val hfToken: String,
    /** JSON object: tool id -> enabled. */
    val toolEnabledJson: String,
    val lastModelId: String?,
    val lastAgentId: String?,
    @ColumnInfo(defaultValue = "0")
    val wifiOnlyDownloads: Boolean = false,
)
