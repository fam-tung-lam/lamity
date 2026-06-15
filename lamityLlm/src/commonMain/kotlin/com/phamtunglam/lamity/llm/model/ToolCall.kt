package com.phamtunglam.lamity.llm.model

import kotlinx.serialization.json.JsonObject

/** A tool/function call requested by the model. */
data class ToolCall(val name: String, val arguments: JsonObject)
