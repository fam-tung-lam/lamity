package com.phamtunglam.lamity.llm.tool

import com.phamtunglam.lamity.llm.LiteRtLmException
import com.phamtunglam.lamity.llm.model.Content
import com.phamtunglam.lamity.llm.model.Contents
import com.phamtunglam.lamity.llm.model.Message
import com.phamtunglam.lamity.llm.model.ToolCall
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray

/**
 * Holds the tools for a conversation and runs them by name.
 */
class ToolManager(tools: List<Tool> = emptyList()) {
    private val toolsByName: Map<String, Tool> =
        tools.associateBy { tool ->
            tool.toolName().ifBlank {
                throw LiteRtLmException("Tool description must contain [\"function\"][\"name\"]")
            }
        }

    /** JSON array of every tool description, passed to the native conversation config. */
    val toolsJsonDescription: String =
        buildJsonArray { tools.forEach { add(it.getToolDescription()) } }.toString()

    val isEmpty: Boolean get() = toolsByName.isEmpty()

    suspend fun execute(name: String, arguments: JsonObject): JsonElement {
        val tool = toolsByName[name] ?: throw LiteRtLmException("Tool \"$name\" not found")
        return tool.execute(arguments)
    }

    /**
     * Executes every call and assembles a single tool-response [Message].
     */
    internal suspend fun handleToolCalls(calls: List<ToolCall>): Message {
        val responses =
            calls.map { call ->
                val result =
                    try {
                        execute(call.name, call.arguments)
                    } catch (e: LiteRtLmException) {
                        throw e
                    } catch (
                        @Suppress("TooGenericExceptionCaught") e: Exception,
                    ) {
                        throw LiteRtLmException("Tool \"${call.name}\" execution failed: ${e.message}", e)
                    }
                Content.ToolResponse(call.name, result)
            }
        return Message.tool(Contents(responses))
    }
}
