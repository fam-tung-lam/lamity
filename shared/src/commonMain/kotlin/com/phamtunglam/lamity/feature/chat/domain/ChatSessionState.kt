package com.phamtunglam.lamity.feature.chat.domain

import com.phamtunglam.lamity.feature.models.domain.ModelConfig

/**
 * The live state of the chat screen. Owned by the chat ViewModel for the lifetime of the screen;
 * model/agent selection is restored from settings, the message thread from the opened conversation.
 */
data class ChatSessionState(
    val conversationId: String? = null,
    val agentId: String? = null,
    val modelId: String? = null,
    /**
     * Effective inference config in use. For an agent it is derived from the agent (its override or
     * its model's catalog default); for an agent-less chat it is the model's catalog default, freely
     * adjustable in the customize sheet but never persisted.
     */
    val runtimeConfig: ModelConfig = ModelConfig(),
    /** Per-chat system prompt when running without an agent (in-memory only). */
    val customSystemPrompt: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val streamingText: String = "",
    val streamingThought: String = "",
    val isGenerating: Boolean = false,
    val engine: EngineState = EngineState.Idle,
    val error: ChatError? = null,
    val notice: ChatNotice? = null,
)
