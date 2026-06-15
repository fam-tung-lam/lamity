package com.phamtunglam.lamity.llm.model

import com.phamtunglam.lamity.llm.tool.Tool
import kotlinx.serialization.json.JsonObject

/** Configuration for creating a Conversation. */
class ConversationConfig(
    val systemMessage: Message? = null,
    val initialMessages: List<Message> = emptyList(),
    val tools: List<Tool> = emptyList(),
    val samplerConfig: SamplerConfig? = null,
    val loraConfig: LoraConfig? = null,
    /** Extra context merged into the prompt template when the conversation is created. */
    val extraContext: JsonObject? = null,
    /**
     * Whether requested tools are executed automatically by the conversation's tool loop. When
     * `false`, tool calls are surfaced to the caller (in the streamed/returned [Message]) instead.
     */
    val automaticToolCalling: Boolean = true,
)
