package com.phamtunglam.lamity.feature.models.domain

import kotlinx.serialization.Serializable

@Serializable
enum class LlmBackend { CPU, GPU }

/** Per-model inference configuration. */
@Serializable
data class ModelConfig(
    val backend: LlmBackend = LlmBackend.GPU,
    val maxTokens: Int = 2048,
    val topK: Int = 40,
    val topP: Double = 0.95,
    val temperature: Double = 0.8,
)

/** A catalog entry: a .litertlm model that can be downloaded and run on-device. */
@Serializable
data class LlmModel(
    val id: String,
    val name: String,
    val description: String = "",
    val url: String,
    val fileName: String,
    val sizeBytes: Long = 0,
    val requiresAuth: Boolean = false,
    val isCustom: Boolean = false,
    val supportsThinking: Boolean = false,
    /** Whether the model can use tools / skills; gates the agent-wizard steps. */
    val supportsTools: Boolean = true,
    val learnMoreUrl: String = "",
    val config: ModelConfig = ModelConfig(),
    val defaultConfig: ModelConfig = ModelConfig(),
)
