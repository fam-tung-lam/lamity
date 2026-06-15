package com.phamtunglam.lamity.feature.agents.domain

import com.phamtunglam.lamity.feature.models.domain.ModelConfig
import kotlinx.serialization.Serializable

/** An AI agent: persona + model + system prompt + attached tools and skills. */
@Serializable
data class Agent(
    val id: String,
    val name: String,
    val description: String = "",
    val systemPrompt: String = "",
    val toolIds: List<String> = emptyList(),
    val skillIds: List<String> = emptyList(),
    /** Model this agent runs on. */
    val modelId: String,
    /** Inference config override; null inherits the model's catalog-default config. */
    val modelConfig: ModelConfig? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)
