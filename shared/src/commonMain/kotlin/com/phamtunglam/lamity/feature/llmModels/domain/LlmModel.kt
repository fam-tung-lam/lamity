package com.phamtunglam.lamity.feature.llmModels.domain

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
    val supportsThinking: Boolean = false,
    /** Whether the model can use tools / skills; when false the chat attaches neither. */
    val supportsTools: Boolean = true,
    val learnMoreUrl: String = "",
    /**
     * Catalog default inference config for this model. Read-only and never persisted per model — it
     * seeds the in-memory config used for a chat, which can then adjust it in the chat settings sheet.
     */
    val config: ModelConfig = ModelConfig(),
)
