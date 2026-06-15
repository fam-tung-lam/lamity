package com.phamtunglam.lamity.llm.native

import com.google.ai.edge.litertlm.Conversation as SdkConversation
import com.google.ai.edge.litertlm.ConversationConfig as SdkConversationConfig
import com.google.ai.edge.litertlm.Engine as SdkEngine
import com.google.ai.edge.litertlm.EngineConfig as SdkEngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.LogSeverity
import com.google.ai.edge.litertlm.Message as SdkMessage
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.OpenApiTool
import com.google.ai.edge.litertlm.SamplerConfig as SdkSamplerConfig
import com.phamtunglam.lamity.llm.model.EngineConfig
import com.phamtunglam.lamity.llm.model.Message
import com.phamtunglam.lamity.llm.model.SamplerConfig
import kotlinx.serialization.json.JsonObject

internal actual fun createNativeRuntime(): LiteRtLmNativeRuntime = AndroidLiteRtLmRuntime()

/**
 * [LiteRtLmNativeRuntime] over the `com.google.ai.edge.litertlm` Kotlin SDK. Tool calls are
 * surfaced (never executed natively): `automaticToolCalling = false`, tools are registered as
 * schema-only [OpenApiTool]s, and the common [com.phamtunglam.lamity.llm.Conversation] runs
 * the tool loop.
 */
@OptIn(ExperimentalApi::class)
internal class AndroidLiteRtLmRuntime : LiteRtLmNativeRuntime {
    override fun createEngine(config: EngineConfig): EngineHandle {
        val sdkConfig =
            SdkEngineConfig(
                modelPath = config.modelPath,
                backend = config.backend.toSdk(),
                visionBackend = config.visionBackend?.toSdk(),
                audioBackend = config.audioBackend?.toSdk(),
                maxNumTokens = config.maxNumTokens,
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
        toolsJson: String,
    ): ConversationHandle {
        val sdkEngine = engine.native as SdkEngine
        val config =
            SdkConversationConfig(
                systemInstruction = systemMessage?.contents?.toSdk(),
                initialMessages = initialMessages.map { it.toSdk() },
                tools = parseTools(toolsJson),
                samplerConfig =
                    samplerConfig?.let {
                        SdkSamplerConfig(it.topK, it.topP, it.temperature, it.seed)
                    },
                automaticToolCalling = false,
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

    override fun renderMessage(conversation: ConversationHandle, message: Message): String =
        (conversation.native as SdkConversation).renderMessageIntoString(message.toSdk(), emptyMap())

    @Suppress("MagicNumber") // 0..5 map to the LiteRT-LM log-severity levels.
    override fun setMinLogLevel(level: Int) {
        SdkEngine.setNativeMinLogSeverity(
            when {
                level <= 0 -> LogSeverity.VERBOSE
                level == 1 -> LogSeverity.DEBUG
                level == 2 -> LogSeverity.INFO
                level == 3 -> LogSeverity.WARNING
                level == 4 -> LogSeverity.ERROR
                level == 5 -> LogSeverity.FATAL
                else -> LogSeverity.INFINITY
            },
        )
    }
}
