package com.phamtunglam.lamity.llm.model

import com.phamtunglam.lamity.llm.tool.Tool

/** Configuration for creating a Conversation. */
class ConversationConfig(
    val systemMessage: Message? = null,
    val initialMessages: List<Message> = emptyList(),
    val tools: List<Tool> = emptyList(),
    val samplerConfig: SamplerConfig? = null,
)
