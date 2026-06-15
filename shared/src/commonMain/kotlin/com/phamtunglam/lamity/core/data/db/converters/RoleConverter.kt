package com.phamtunglam.lamity.core.data.db.converters

import androidx.room3.TypeConverter
import com.phamtunglam.lamity.llm.model.Role

/**
 * Persists the lamityLlm [Role] enum as its stable `jsonName` string, so the Kotlin side keeps
 * working with the real enum instead of a raw primitive and the schema stays readable.
 */
class RoleConverter {
    @TypeConverter
    fun roleToString(role: Role): String = role.jsonName

    @TypeConverter
    fun roleFromString(value: String): Role = Role.fromJsonName(value)
}
