package com.phamtunglam.lamity.llm.native

import com.phamtunglam.lamity.llm.model.BenchmarkInfo
import com.phamtunglam.lamity.llm.model.EngineConfig
import com.phamtunglam.lamity.llm.model.InputData
import com.phamtunglam.lamity.llm.model.LogSeverity
import com.phamtunglam.lamity.llm.model.LoraConfig
import com.phamtunglam.lamity.llm.model.Message
import com.phamtunglam.lamity.llm.model.SamplerConfig
import kotlinx.serialization.json.JsonObject

/**
 * Thin per-platform contract over the native LiteRT-LM runtime.
 *
 * Android implements it over the `com.google.ai.edge.litertlm` Kotlin SDK; iOS implements it via
 * Kotlin/Native cinterop to the `CLiteRTLM` C API.
 */
@Suppress("TooManyFunctions") // Wide but flat dispatch surface mirroring the native C API / SDK.
internal interface LiteRtLmNativeRuntime {
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

    fun deleteConversation(handle: ConversationHandle)

    fun streamTurn(
        conversation: ConversationHandle,
        message: Message,
        extraContext: JsonObject?,
        callback: TurnCallback,
    )

    fun cancel(conversation: ConversationHandle)

    fun getTokenCount(conversation: ConversationHandle): Int

    fun getBenchmarkInfo(conversation: ConversationHandle): BenchmarkInfo

    fun renderMessage(conversation: ConversationHandle, message: Message): String

    fun createSession(engine: EngineHandle, samplerConfig: SamplerConfig?, loraConfig: LoraConfig?): SessionHandle

    fun deleteSession(handle: SessionHandle)

    fun runSessionPrefill(session: SessionHandle, inputData: List<InputData>)

    fun runSessionDecode(session: SessionHandle): String

    fun generateSessionContent(session: SessionHandle, inputData: List<InputData>): String

    fun generateSessionContentStream(
        session: SessionHandle,
        inputData: List<InputData>,
        callback: SessionStreamCallback,
    )

    fun cancelSession(session: SessionHandle)

    fun createCapabilities(modelPath: String): CapabilitiesHandle

    fun deleteCapabilities(handle: CapabilitiesHandle)

    fun hasSpeculativeDecodingSupport(handle: CapabilitiesHandle): Boolean

    fun benchmark(config: EngineConfig, prefillTokens: Int, decodeTokens: Int): BenchmarkInfo

    fun setMinLogLevel(severity: LogSeverity)
}
