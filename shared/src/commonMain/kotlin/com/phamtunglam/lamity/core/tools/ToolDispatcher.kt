package com.phamtunglam.lamity.core.tools

import com.phamtunglam.lamity.core.platform.epochMillis
import com.phamtunglam.lamity.core.platform.newId
import com.phamtunglam.lamity.llm.ToolExecutor
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

/**
 * Executes built-in tools on behalf of the platform LLM runtimes. Called from
 * whatever thread LiteRT-LM invokes tools on; everything inside is thread-safe.
 */
class ToolDispatcher(private val registry: ToolRegistry) : ToolExecutor {

    private val _events = MutableSharedFlow<ToolEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<ToolEvent> = _events.asSharedFlow()

    override fun executeTool(toolId: String, paramsJson: String): String {
        val args = runCatching {
            Json.parseToJsonElement(paramsJson.ifBlank { "{}" }) as? JsonObject
        }.getOrNull() ?: buildJsonObject { }

        val result = registry.execute(toolId, args)
        val resultText = result.toString()
        _events.tryEmit(
            ToolEvent(
                id = newId(),
                toolId = toolId,
                argsJson = paramsJson,
                resultJson = resultText,
                atMillis = epochMillis(),
            )
        )
        return resultText
    }
}
