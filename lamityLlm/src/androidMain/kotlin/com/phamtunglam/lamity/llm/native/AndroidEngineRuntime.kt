@file:OptIn(ExperimentalApi::class)

package com.phamtunglam.lamity.llm.native

import com.google.ai.edge.litertlm.ConversationConfig as SdkConversationConfig
import com.google.ai.edge.litertlm.Engine as SdkEngine
import com.google.ai.edge.litertlm.EngineConfig as SdkEngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.SamplerConfig as SdkSamplerConfig
import com.google.ai.edge.litertlm.SessionConfig as SdkSessionConfig
import com.phamtunglam.lamity.llm.model.EngineConfig
import com.phamtunglam.lamity.llm.model.LoraConfig
import com.phamtunglam.lamity.llm.model.Message
import com.phamtunglam.lamity.llm.model.SamplerConfig
import kotlinx.serialization.json.JsonObject

internal actual fun createEngineNativeRuntime(): EngineNativeRuntime = AndroidEngineRuntime()

/**
 * [EngineNativeRuntime] over the `com.google.ai.edge.litertlm` Kotlin SDK. Conversations are created
 * with `automaticToolCalling = false` and schema-only tools, so the common
 * [com.phamtunglam.lamity.llm.Conversation] runs the tool loop.
 */
internal class AndroidEngineRuntime : EngineNativeRuntime {
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

    override fun createSession(
        engine: EngineHandle,
        samplerConfig: SamplerConfig?,
        loraConfig: LoraConfig?,
    ): SessionHandle {
        val sdkEngine = engine.native as SdkEngine
        val config = SdkSessionConfig(samplerConfig = samplerConfig?.toSdk(), loraConfig = loraConfig?.toSdk())
        return SessionHandle(sdkEngine.createSession(config))
    }
}

private fun SamplerConfig.toSdk(): SdkSamplerConfig = SdkSamplerConfig(topK, topP, temperature, seed)
