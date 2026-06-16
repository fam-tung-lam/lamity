package com.phamtunglam.lamity.feature.tools.domain

import com.phamtunglam.lamity.llm.tool.Tool
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * Base for the app's built-in tools. It implements the LiteRT-LM [Tool] contract directly, so an
 * instance can be passed straight into a [com.phamtunglam.lamity.llm.model.ConversationConfig], and it
 * additionally carries the [id]/[displayName]/[description] the chat settings sheet needs to list and
 * toggle tools.
 *
 * Subclasses implement [perform]; this base assembles the OpenAI-style function schema, converts any
 * thrown error into a result the model can read, and forwards each completed call to [onInvoked] so
 * the chat can record it.
 */
abstract class AppTool(
    val id: String,
    val displayName: String,
    val description: String,
    private val parameters: JsonObject,
) : Tool {
    /**
     * Sink notified after each call with the tool [id] and the argument/result JSON. The chat
     * session wires this up to persist a tool message; it stays null wherever tools are only listed.
     */
    var onInvoked: ((toolName: String, argsJson: String, resultJson: String) -> Unit)? = null

    final override fun getToolDescription(): JsonObject =
        buildJsonObject {
            put("type", "function")
            putJsonObject("function") {
                put("name", id)
                put("description", description)
                put("parameters", parameters)
            }
        }

    final override suspend fun execute(arguments: JsonObject): JsonElement {
        val result =
            try {
                perform(arguments)
            } catch (e: CancellationException) {
                throw e
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                buildJsonObject { put("error", e.message ?: "Tool execution failed") }
            }
        onInvoked?.invoke(id, arguments.toString(), result.toString())
        return result
    }

    /** Runs the tool body. Unexpected exceptions are reported back to the model as an error result. */
    protected abstract suspend fun perform(arguments: JsonObject): JsonElement
}
