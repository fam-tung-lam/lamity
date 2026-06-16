package com.phamtunglam.lamity.feature.chat.domain

import com.phamtunglam.lamity.feature.models.domain.ModelConfig

/**
 * The live state of the chat screen. Owned by the chat ViewModel for the lifetime of the screen.
 * The model selection is restored from settings; the message thread from the opened conversation.
 * Inference config, the tool/skill toggles and the system prompt are in-memory per chat (not saved).
 */
data class ChatSessionState(
    val conversationId: String? = null,
    val modelId: String? = null,
    /** Effective inference config: the selected model's catalog default, adjustable in the sheet. */
    val runtimeConfig: ModelConfig = ModelConfig(),
    /** Built-in tool ids enabled for this chat (default all). Ignored when the model has no tools. */
    val enabledToolIds: Set<String> = emptySet(),
    /** Built-in skill ids enabled for this chat (default all). Ignored when the model has no tools. */
    val enabledSkillIds: Set<String> = emptySet(),
    /** Per-chat system prompt (in-memory only). */
    val customSystemPrompt: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val streamingText: String = "",
    val streamingThought: String = "",
    val isGenerating: Boolean = false,
    val engine: EngineState = EngineState.Idle,
    val error: ChatError? = null,
    val notice: ChatNotice? = null,
)
