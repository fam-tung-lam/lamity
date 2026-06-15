package com.phamtunglam.lamity.llm.serialization

import com.phamtunglam.lamity.llm.model.Content
import com.phamtunglam.lamity.llm.model.Contents
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
internal fun Content.toJson(): JsonObject =
    when (this) {
        is Content.Text -> {
            buildJsonObject {
                put("type", "text")
                put("text", text)
            }
        }

        is Content.ImageBytes -> {
            buildJsonObject {
                put("type", "image")
                put("blob", Base64.encode(bytes))
            }
        }

        is Content.ImageFile -> {
            buildJsonObject {
                put("type", "image")
                put("path", path)
            }
        }

        is Content.AudioBytes -> {
            buildJsonObject {
                put("type", "audio")
                put("blob", Base64.encode(bytes))
            }
        }

        is Content.AudioFile -> {
            buildJsonObject {
                put("type", "audio")
                put("path", path)
            }
        }

        is Content.ToolResponse -> {
            buildJsonObject {
                put("type", "tool_response")
                put("name", name)
                put("response", response ?: JsonNull)
            }
        }
    }

internal fun Contents.toJsonArray(): JsonArray = buildJsonArray { values.forEach { add(it.toJson()) } }

/** Serialized content array (for the system message). */
internal fun Contents.toJsonArrayString(): String = toJsonArray().toString()

@OptIn(ExperimentalEncodingApi::class)
internal fun contentFromJson(obj: JsonObject): Content? =
    when (obj["type"]?.jsonPrimitive?.contentOrNull) {
        "text" -> {
            Content.Text(obj["text"]?.jsonPrimitive?.contentOrNull ?: "")
        }

        "image" -> {
            obj["blob"]?.jsonPrimitive?.contentOrNull?.let { Content.ImageBytes(Base64.decode(it)) }
                ?: obj["path"]?.jsonPrimitive?.contentOrNull?.let { Content.ImageFile(it) }
        }

        "audio" -> {
            obj["blob"]?.jsonPrimitive?.contentOrNull?.let { Content.AudioBytes(Base64.decode(it)) }
                ?: obj["path"]?.jsonPrimitive?.contentOrNull?.let { Content.AudioFile(it) }
        }

        "tool_response" -> {
            Content.ToolResponse(obj["name"]?.jsonPrimitive?.contentOrNull ?: "", obj["response"])
        }

        else -> {
            null
        }
    }
