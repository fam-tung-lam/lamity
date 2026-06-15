package com.phamtunglam.lamity.llm.native

import com.phamtunglam.lamity.llm.model.EngineConfig
import com.phamtunglam.lamity.llm.model.LoraConfig
import com.phamtunglam.lamity.llm.model.Message
import com.phamtunglam.lamity.llm.model.SamplerConfig
import kotlinx.serialization.json.JsonObject

/** Opaque platform engine handle (Android `Engine` / iOS C engine pointer). */
internal class EngineHandle(val native: Any)

internal expect fun createEngineNativeRuntime(): EngineNativeRuntime

/**
 * Engine lifecycle plus the creation of the conversation and session handles spawned from an engine.
 */
internal interface EngineNativeRuntime {
    fun createEngine(config: EngineConfig): EngineHandle

    fun deleteEngine(handle: EngineHandle)

    fun createConversation(
        engine: EngineHandle,
        systemMessage: Message?,
        initialMessages: List<Message>,
        samplerConfig: SamplerConfig?,
        loraConfig: LoraConfig?,
        extraContext: JsonObject?,
        toolsJson: String,
    ): ConversationHandle

    fun createSession(engine: EngineHandle, samplerConfig: SamplerConfig?, loraConfig: LoraConfig?): SessionHandle
}
