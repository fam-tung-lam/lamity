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

/**
 * A conversation is a pure message thread. What it runs with (agent, model, an agent-less system
 * prompt) is the live app selection, never stored here.
 */
@Serializable
data class Conversation(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
)
