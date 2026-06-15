package com.phamtunglam.lamity.llm.model

/** Configuration for creating a [com.phamtunglam.lamity.llm.Session]. */
data class SessionConfig(val samplerConfig: SamplerConfig? = null, val loraConfig: LoraConfig? = null)
