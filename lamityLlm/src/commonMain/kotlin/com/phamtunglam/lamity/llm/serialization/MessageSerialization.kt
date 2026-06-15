package com.phamtunglam.lamity.llm.serialization

import com.phamtunglam.lamity.llm.model.Contents
import com.phamtunglam.lamity.llm.model.Message
import com.phamtunglam.lamity.llm.model.Role
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

internal fun Message.toJsonObject(): JsonObject =
    buildJsonObject {
        put("role", role.jsonName)
        if (!contents.isEmpty) put("content", contents.toJsonArray())
        if (toolCalls.isNotEmpty()) put("tool_calls", buildJsonArray { toolCalls.forEach { add(it.toJson()) } })
        if (channels.isNotEmpty()) {
            putJsonObject("channels") { channels.forEach { (key, value) -> put(key, value) } }
        }
    }

/** Serialized message object: `{"role":..,"content":[..],"tool_calls":[..],"channels":{..}}`. */
internal fun Message.toJsonString(): String = toJsonObject().toString()

/** Serialized array of message objects (for initial messages). */
internal fun List<Message>.toJsonArrayString(): String =
    buildJsonArray { this@toJsonArrayString.forEach { add(it.toJsonObject()) } }.toString()

internal fun messageFromJson(obj: JsonObject): Message {
    val role = Role.fromJsonName(obj["role"]?.jsonPrimitive?.contentOrNull)
    val contents =
        (obj["content"] as? JsonArray)?.let { array ->
            Contents(array.mapNotNull { contentFromJson(it.jsonObject) })
        } ?: Contents.empty
    val toolCalls = (obj["tool_calls"] as? JsonArray)?.let(::toolCallsFromJson) ?: emptyList()
    val channels =
        (obj["channels"] as? JsonObject)
            ?.entries
            ?.mapNotNull { (key, value) -> (value as? JsonPrimitive)?.contentOrNull?.let { key to it } }
            ?.toMap()
            ?: emptyMap()
    return Message(role, contents, toolCalls, channels)
}

internal fun messageFromJsonString(json: String): Message =
    messageFromJson(LiteRtLmJson.parseToJsonElement(json).jsonObject)
