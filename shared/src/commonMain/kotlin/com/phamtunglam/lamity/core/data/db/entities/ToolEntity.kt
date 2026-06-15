package com.phamtunglam.lamity.core.data.db.entities

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Catalog row for a built-in tool. The executable behaviour lives in code (the `AppTool` instances);
 * this table exists so agents can reference tools relationally ([AgentToolCrossRef]). A tool is
 * "off" until an agent links it — there is no global enable flag.
 */
@Entity(tableName = "tools")
data class ToolEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String,
)
