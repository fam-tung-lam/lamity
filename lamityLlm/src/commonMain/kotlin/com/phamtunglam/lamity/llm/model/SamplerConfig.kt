package com.phamtunglam.lamity.llm.model

/** Sampling configuration for a conversation. */
data class SamplerConfig(
    val topK: Int,
    val topP: Double,
    val temperature: Double,
    val seed: Int = 0,
)
