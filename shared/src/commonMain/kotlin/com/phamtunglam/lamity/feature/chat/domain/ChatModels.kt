package com.phamtunglam.lamity.feature.chat.domain

import kotlinx.serialization.Serializable

@Serializable
enum class MessageRole { USER, ASSISTANT, TOOL }

@Serializable
data class ChatMessage(
    val id: String,
    val conversationId: String,
    val role: MessageRole,
    val content: String = "",
    /** Reasoning emitted on the model's "thought" channel (DeepSeek-R1 etc.). */
    val thought: String = "",
    val toolName: String = "",
    val toolArgs: String = "",
    val toolResult: String = "",
    val genMillis: Long = 0,
    val tokensPerSec: Double = 0.0,
    val createdAt: Long,
)

@Serializable
data class Conversation(
    val id: String,
    val title: String,
    val agentId: String? = null,
    val modelId: String,
    /** Tool ids for an agent-less customized chat; null = defaults (all enabled tools). */
    val customToolIds: List<String>? = null,
    /** Skill ids for an agent-less customized chat; null = none. */
    val customSkillIds: List<String>? = null,
    /** System prompt for an agent-less customized chat; null = none. */
    val customSystemPrompt: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
