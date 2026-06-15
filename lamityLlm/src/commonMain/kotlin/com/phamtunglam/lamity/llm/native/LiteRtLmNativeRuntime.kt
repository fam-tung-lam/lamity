package com.phamtunglam.lamity.llm.native

import com.phamtunglam.lamity.llm.model.EngineConfig
import com.phamtunglam.lamity.llm.model.Message
import com.phamtunglam.lamity.llm.model.SamplerConfig
import kotlinx.serialization.json.JsonObject

/**
 * Thin per-platform contract over the native LiteRT-LM runtime.
 *
 * Android implements it over the `com.google.ai.edge.litertlm` Kotlin SDK; iOS implements it via
 * Kotlin/Native cinterop to the `CLiteRTLM` C API.
 */
internal interface LiteRtLmNativeRuntime {
    fun createEngine(config: EngineConfig): EngineHandle

    fun deleteEngine(handle: EngineHandle)

    fun createConversation(
        engine: EngineHandle,
        systemMessage: Message?,
        initialMessages: List<Message>,
        samplerConfig: SamplerConfig?,
        toolsJson: String,
    ): ConversationHandle

    fun deleteConversation(handle: ConversationHandle)

    fun streamTurn(
        conversation: ConversationHandle,
        message: Message,
        extraContext: JsonObject?,
        callback: TurnCallback,
    )

    fun cancel(conversation: ConversationHandle)

    fun getTokenCount(conversation: ConversationHandle): Int

    fun renderMessage(conversation: ConversationHandle, message: Message): String

    fun setMinLogLevel(level: Int)
}
