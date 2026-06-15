package com.phamtunglam.lamity.llm.serialization

import kotlinx.serialization.json.Json

/**
 * JSON (de)serialization for the LiteRT-LM native contract.
 */
internal val LiteRtLmJson =
    Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
