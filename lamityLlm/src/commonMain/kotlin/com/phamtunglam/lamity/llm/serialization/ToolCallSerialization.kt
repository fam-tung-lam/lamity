package com.phamtunglam.lamity.llm.serialization

import com.phamtunglam.lamity.llm.model.ToolCall
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

internal fun ToolCall.toJson(): JsonObject =
    buildJsonObject {
        put("type", "function")
        putJsonObject("function") {
            put("name", name)
            put("arguments", arguments)
        }
    }

internal fun toolCallsFromJson(array: JsonArray): List<ToolCall> =
    array.mapNotNull { element ->
        val function = element.jsonObject["function"]?.jsonObject ?: return@mapNotNull null
        val name = function["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        val arguments = function["arguments"] as? JsonObject ?: JsonObject(emptyMap())
        ToolCall(name, arguments)
    }
