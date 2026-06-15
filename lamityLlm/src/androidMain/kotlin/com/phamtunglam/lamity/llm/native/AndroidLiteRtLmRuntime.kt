package com.phamtunglam.lamity.llm.native

import com.google.ai.edge.litertlm.Capabilities as SdkCapabilities
import com.google.ai.edge.litertlm.Conversation as SdkConversation
import com.google.ai.edge.litertlm.ConversationConfig as SdkConversationConfig
import com.google.ai.edge.litertlm.Engine as SdkEngine
import com.google.ai.edge.litertlm.EngineConfig as SdkEngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.LogSeverity as SdkLogSeverity
import com.google.ai.edge.litertlm.Message as SdkMessage
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.ResponseCallback
import com.google.ai.edge.litertlm.SamplerConfig as SdkSamplerConfig
import com.google.ai.edge.litertlm.Session as SdkSession
import com.google.ai.edge.litertlm.SessionConfig as SdkSessionConfig
import com.google.ai.edge.litertlm.benchmark as sdkBenchmark
import com.phamtunglam.lamity.llm.model.BenchmarkInfo
import com.phamtunglam.lamity.llm.model.EngineConfig
import com.phamtunglam.lamity.llm.model.InputData
import com.phamtunglam.lamity.llm.model.LogSeverity
import com.phamtunglam.lamity.llm.model.LoraConfig
import com.phamtunglam.lamity.llm.model.Message
import com.phamtunglam.lamity.llm.model.SamplerConfig
import kotlinx.serialization.json.JsonObject

internal actual fun createNativeRuntime(): LiteRtLmNativeRuntime = AndroidLiteRtLmRuntime()

/**
 * [LiteRtLmNativeRuntime] over the `com.google.ai.edge.litertlm` Kotlin SDK. Tool calls are
 * surfaced (never executed natively): `automaticToolCalling = false`, tools are registered as
 * schema-only [com.google.ai.edge.litertlm.OpenApiTool]s, and the common
 * [com.phamtunglam.lamity.llm.Conversation] runs the tool loop.
 */
@OptIn(ExperimentalApi::class)
@Suppress("TooManyFunctions") // Wide but flat dispatch surface mirroring the native SDK.
internal class AndroidLiteRtLmRuntime : LiteRtLmNativeRuntime {
    override fun createEngine(config: EngineConfig): EngineHandle {
        val sdkConfig =
            SdkEngineConfig(
                modelPath = config.modelPath,
                backend = config.backend.toSdk(),
                visionBackend = config.visionBackend?.toSdk(),
                audioBackend = config.audioBackend?.toSdk(),
                maxNumTokens = config.maxNumTokens,
                maxNumImages = config.maxNumImages,
                cacheDir = config.cacheDir,
            )
        val engine = SdkEngine(sdkConfig)
        engine.initialize()
        return EngineHandle(engine)
    }

    override fun deleteEngine(handle: EngineHandle) {
        (handle.native as SdkEngine).close()
    }

    override fun createConversation(
        engine: EngineHandle,
        systemMessage: Message?,
        initialMessages: List<Message>,
        samplerConfig: SamplerConfig?,
        loraConfig: LoraConfig?,
        extraContext: JsonObject?,
        toolsJson: String,
    ): ConversationHandle {
        val sdkEngine = engine.native as SdkEngine
        val config =
            SdkConversationConfig(
                systemInstruction = systemMessage?.contents?.toSdk(),
                initialMessages = initialMessages.map { it.toSdk() },
                tools = parseTools(toolsJson),
                samplerConfig = samplerConfig?.toSdk(),
                automaticToolCalling = false,
                extraContext = extraContext?.let { jsonObjectToMap(it) } ?: emptyMap(),
                loraConfig = loraConfig?.toSdk(),
            )
        return ConversationHandle(sdkEngine.createConversation(config))
    }

    override fun deleteConversation(handle: ConversationHandle) {
        (handle.native as SdkConversation).close()
    }

    override fun streamTurn(
        conversation: ConversationHandle,
        message: Message,
        extraContext: JsonObject?,
        callback: TurnCallback,
    ) {
        val conv = conversation.native as SdkConversation
        val extra = extraContext?.let { jsonObjectToMap(it) } ?: emptyMap()
        conv.sendMessageAsync(
            message.toSdk(),
            object : MessageCallback {
                override fun onMessage(message: SdkMessage) {
                    callback.onChunk(message.toCommon())
                }

                override fun onDone() {
                    callback.onComplete()
                }

                override fun onError(throwable: Throwable) {
                    callback.onError(throwable.message ?: throwable.toString())
                }
            },
            extra,
        )
    }

    override fun cancel(conversation: ConversationHandle) {
        (conversation.native as SdkConversation).cancelProcess()
    }

    override fun getTokenCount(conversation: ConversationHandle): Int =
        (conversation.native as SdkConversation).getTokenCount()

    override fun getBenchmarkInfo(conversation: ConversationHandle): BenchmarkInfo =
        (conversation.native as SdkConversation).getBenchmarkInfo().toCommon()

    override fun renderMessage(conversation: ConversationHandle, message: Message): String =
        (conversation.native as SdkConversation).renderMessageIntoString(message.toSdk(), emptyMap())

    override fun createSession(
        engine: EngineHandle,
        samplerConfig: SamplerConfig?,
        loraConfig: LoraConfig?,
    ): SessionHandle {
        val sdkEngine = engine.native as SdkEngine
        val config = SdkSessionConfig(samplerConfig = samplerConfig?.toSdk(), loraConfig = loraConfig?.toSdk())
        return SessionHandle(sdkEngine.createSession(config))
    }

    override fun deleteSession(handle: SessionHandle) {
        (handle.native as SdkSession).close()
    }

    override fun runSessionPrefill(session: SessionHandle, inputData: List<InputData>) {
        (session.native as SdkSession).runPrefill(inputData.map { it.toSdk() })
    }

    override fun runSessionDecode(session: SessionHandle): String = (session.native as SdkSession).runDecode()

    override fun generateSessionContent(session: SessionHandle, inputData: List<InputData>): String =
        (session.native as SdkSession).generateContent(inputData.map { it.toSdk() })

    override fun generateSessionContentStream(
        session: SessionHandle,
        inputData: List<InputData>,
        callback: SessionStreamCallback,
    ) {
        (session.native as SdkSession).generateContentStream(
            inputData.map { it.toSdk() },
            object : ResponseCallback {
                override fun onNext(response: String) {
                    callback.onChunk(response)
                }

                override fun onDone() {
                    callback.onComplete()
                }

                override fun onError(throwable: Throwable) {
                    callback.onError(throwable.message ?: throwable.toString())
                }
            },
        )
    }

    override fun cancelSession(session: SessionHandle) {
        (session.native as SdkSession).cancelProcess()
    }

    override fun createCapabilities(modelPath: String): CapabilitiesHandle =
        CapabilitiesHandle(SdkCapabilities(modelPath))

    override fun deleteCapabilities(handle: CapabilitiesHandle) {
        (handle.native as SdkCapabilities).close()
    }

    override fun hasSpeculativeDecodingSupport(handle: CapabilitiesHandle): Boolean =
        (handle.native as SdkCapabilities).hasSpeculativeDecodingSupport()

    override fun benchmark(config: EngineConfig, prefillTokens: Int, decodeTokens: Int): BenchmarkInfo =
        sdkBenchmark(config.modelPath, config.backend.toSdk(), prefillTokens, decodeTokens, config.cacheDir).toCommon()

    @Suppress("MagicNumber") // 0..5 map to the LiteRT-LM log-severity levels.
    override fun setMinLogLevel(severity: LogSeverity) {
        SdkEngine.setNativeMinLogSeverity(
            when (severity.level) {
                0 -> SdkLogSeverity.VERBOSE
                1 -> SdkLogSeverity.DEBUG
                2 -> SdkLogSeverity.INFO
                3 -> SdkLogSeverity.WARNING
                4 -> SdkLogSeverity.ERROR
                5 -> SdkLogSeverity.FATAL
                else -> SdkLogSeverity.INFINITY
            },
        )
    }
}

private fun SamplerConfig.toSdk(): SdkSamplerConfig = SdkSamplerConfig(topK, topP, temperature, seed)
