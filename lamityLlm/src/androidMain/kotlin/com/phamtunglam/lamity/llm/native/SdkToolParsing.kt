package com.phamtunglam.lamity.llm.native

import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.OpenApiTool
import com.google.ai.edge.litertlm.ToolProvider
import com.google.ai.edge.litertlm.tool
import com.phamtunglam.lamity.llm.serialization.LiteRtLmJson
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray

/**
 * Parses the common-layer tools JSON into schema-only [OpenApiTool]s. Tool calls are surfaced
 * (never executed natively): `automaticToolCalling = false`, so [OpenApiTool.execute] is never
 * invoked and the common [com.phamtunglam.lamity.llm.Conversation] runs the tool loop.
 */
@OptIn(ExperimentalApi::class)
internal fun parseTools(toolsJson: String): List<ToolProvider> {
    if (toolsJson.isBlank()) return emptyList()
    val array =
        runCatching { LiteRtLmJson.parseToJsonElement(toolsJson).jsonArray }.getOrNull() ?: return emptyList()
    return array.mapNotNull { element ->
        val obj = element as? JsonObject ?: return@mapNotNull null
        val function = (obj["function"] as? JsonObject) ?: obj
        tool(
            object : OpenApiTool {
                override fun getToolDescriptionJsonString(): String = function.toString()

                // Never invoked: automaticToolCalling = false; the common layer executes tools.
                override fun execute(paramsJsonString: String): String =
                    """{"error":"tool execution is handled by the common layer"}"""
            },
        )
    }
}
