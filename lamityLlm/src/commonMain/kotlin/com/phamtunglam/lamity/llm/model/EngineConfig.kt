package com.phamtunglam.lamity.llm.model

/** Configuration for creating an Engine. */
data class EngineConfig(
    val modelPath: String,
    val backend: Backend = Backend.Cpu(),
    val visionBackend: Backend? = null,
    val audioBackend: Backend? = null,
    val maxNumTokens: Int? = null,
    val maxNumImages: Int? = null,
    val cacheDir: String? = null,
)
