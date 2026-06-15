package com.phamtunglam.lamity.llm.native

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

internal fun jsonObjectToMap(obj: JsonObject): Map<String, Any> =
    obj.entries.mapNotNull { (key, value) -> jsonElementToAny(value)?.let { key to it } }.toMap()

internal fun mapToJsonObject(map: Map<String, *>): JsonObject =
    buildJsonObject { map.forEach { (key, value) -> put(key, anyToJsonElement(value)) } }

internal fun jsonElementToAny(element: JsonElement?): Any? =
    when (element) {
        null, JsonNull -> {
            null
        }

        is JsonObject -> {
            element.mapValues { jsonElementToAny(it.value) }
        }

        is JsonArray -> {
            element.map { jsonElementToAny(it) }
        }

        is JsonPrimitive -> {
            when {
                element.isString -> element.content
                element.booleanOrNull != null -> element.booleanOrNull
                element.longOrNull != null -> element.longOrNull
                element.doubleOrNull != null -> element.doubleOrNull
                else -> element.content
            }
        }
    }

internal fun anyToJsonElement(value: Any?): JsonElement =
    when (value) {
        null -> JsonNull
        is JsonElement -> value
        is String -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        is Map<*, *> -> buildJsonObject { value.forEach { (key, item) -> put(key.toString(), anyToJsonElement(item)) } }
        is Iterable<*> -> buildJsonArray { value.forEach { add(anyToJsonElement(it)) } }
        else -> JsonPrimitive(value.toString())
    }
