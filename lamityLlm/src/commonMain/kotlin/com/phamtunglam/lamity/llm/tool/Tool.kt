package com.phamtunglam.lamity.llm.tool

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A tool the model can call.
 */
interface Tool {
    /**
     * The function-schema description, e.g.
     * `{"type":"function","function":{"name":..,"description":..,"parameters":..}}`.
     */
    fun getToolDescription(): JsonObject

    /** Executes the tool with the given [arguments] and returns the JSON result. */
    suspend fun execute(arguments: JsonObject): JsonElement
}

internal fun Tool.toolName(): String =
    getToolDescription()["function"]
        ?.jsonObject
        ?.get("name")
        ?.jsonPrimitive
        ?.contentOrNull ?: ""
