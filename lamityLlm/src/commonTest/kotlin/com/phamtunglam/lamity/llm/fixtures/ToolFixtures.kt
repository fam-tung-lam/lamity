package com.phamtunglam.lamity.llm.fixtures

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/** A `function`-typed tool description carrying [name], as the LiteRT-LM wire format expects. */
internal fun fakeToolDescription(name: String = "echo"): JsonObject =
    buildJsonObject {
        put("type", "function")
        putJsonObject("function") { put("name", name) }
    }
