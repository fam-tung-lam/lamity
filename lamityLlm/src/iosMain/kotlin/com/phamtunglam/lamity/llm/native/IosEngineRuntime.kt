@file:OptIn(ExperimentalForeignApi::class)

package com.phamtunglam.lamity.llm.native

import cnames.structs.LiteRtLmEngine
import com.phamtunglam.lamity.llm.LiteRtLmException
import com.phamtunglam.lamity.llm.cinterop.LiteRtLmSamplerParams
import com.phamtunglam.lamity.llm.cinterop.kLiteRtLmSamplerTypeTopP
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_config_create
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_config_delete
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_config_set_enable_constrained_decoding
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_config_set_extra_context
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_config_set_messages
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_config_set_session_config
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_config_set_system_message
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_config_set_tools
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_create
import com.phamtunglam.lamity.llm.cinterop.litert_lm_engine_create
import com.phamtunglam.lamity.llm.cinterop.litert_lm_engine_create_session
import com.phamtunglam.lamity.llm.cinterop.litert_lm_engine_delete
import com.phamtunglam.lamity.llm.cinterop.litert_lm_engine_settings_delete
import com.phamtunglam.lamity.llm.cinterop.litert_lm_session_config_create
import com.phamtunglam.lamity.llm.cinterop.litert_lm_session_config_delete
import com.phamtunglam.lamity.llm.cinterop.litert_lm_session_config_set_audio_lora_path
import com.phamtunglam.lamity.llm.cinterop.litert_lm_session_config_set_lora_path
import com.phamtunglam.lamity.llm.cinterop.litert_lm_session_config_set_sampler_params
import com.phamtunglam.lamity.llm.model.EngineConfig
import com.phamtunglam.lamity.llm.model.LoraConfig
import com.phamtunglam.lamity.llm.model.Message
import com.phamtunglam.lamity.llm.model.SamplerConfig
import com.phamtunglam.lamity.llm.serialization.toJsonArrayString
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.serialization.json.JsonObject

internal actual fun createEngineNativeRuntime(): EngineNativeRuntime = IosEngineRuntime()

/**
 * [EngineNativeRuntime] over the `CLiteRTLM` C API via Kotlin/Native cinterop. Tool calls are
 * surfaced to the common [com.phamtunglam.lamity.llm.Conversation] rather than executed natively.
 */
@Suppress("UNCHECKED_CAST")
internal class IosEngineRuntime : EngineNativeRuntime {
    override fun createEngine(config: EngineConfig): EngineHandle {
        val settings = buildEngineSettings(config)
        val engine = litert_lm_engine_create(settings)
        litert_lm_engine_settings_delete(settings)
        return EngineHandle(engine ?: throw LiteRtLmException("Failed to create engine"))
    }

    override fun deleteEngine(handle: EngineHandle) {
        litert_lm_engine_delete(handle.native as CPointer<LiteRtLmEngine>)
    }

    override fun createConversation(
        engine: EngineHandle,
        systemMessage: Message?,
        initialMessages: List<Message>,
        samplerConfig: SamplerConfig?,
        loraConfig: LoraConfig?,
        extraContext: JsonObject?,
        toolsJson: String,
    ): ConversationHandle =
        memScoped {
            val enginePtr = engine.native as CPointer<LiteRtLmEngine>
            val sessionConfig =
                litert_lm_session_config_create()
                    ?: throw LiteRtLmException("Failed to create session config")
            samplerConfig?.let {
                litert_lm_session_config_set_sampler_params(sessionConfig, samplerParamsPtr(it))
            }
            loraConfig?.loraPath?.let { litert_lm_session_config_set_lora_path(sessionConfig, it) }
            loraConfig?.audioLoraPath?.let { litert_lm_session_config_set_audio_lora_path(sessionConfig, it) }

            val convConfig =
                litert_lm_conversation_config_create()
                    ?: run {
                        litert_lm_session_config_delete(sessionConfig)
                        throw LiteRtLmException("Failed to create conversation config")
                    }
            litert_lm_conversation_config_set_session_config(convConfig, sessionConfig)
            systemMessage?.let {
                litert_lm_conversation_config_set_system_message(convConfig, it.contents.toJsonArrayString())
            }
            if (toolsJson.isNotBlank() && toolsJson != "[]") {
                litert_lm_conversation_config_set_tools(convConfig, toolsJson)
            }
            if (initialMessages.isNotEmpty()) {
                litert_lm_conversation_config_set_messages(convConfig, initialMessages.toJsonArrayString())
            }
            extraContext?.let { litert_lm_conversation_config_set_extra_context(convConfig, it.toString()) }
            litert_lm_conversation_config_set_enable_constrained_decoding(convConfig, false)

            val conversation = litert_lm_conversation_create(enginePtr, convConfig)
            litert_lm_conversation_config_delete(convConfig)
            litert_lm_session_config_delete(sessionConfig)
            ConversationHandle(conversation ?: throw LiteRtLmException("Failed to create conversation"))
        }

    override fun createSession(
        engine: EngineHandle,
        samplerConfig: SamplerConfig?,
        loraConfig: LoraConfig?,
    ): SessionHandle =
        memScoped {
            val enginePtr = engine.native as CPointer<LiteRtLmEngine>
            val sessionConfig =
                litert_lm_session_config_create()
                    ?: throw LiteRtLmException("Failed to create session config")
            samplerConfig?.let {
                litert_lm_session_config_set_sampler_params(sessionConfig, samplerParamsPtr(it))
            }
            loraConfig?.loraPath?.let { litert_lm_session_config_set_lora_path(sessionConfig, it) }
            loraConfig?.audioLoraPath?.let { litert_lm_session_config_set_audio_lora_path(sessionConfig, it) }
            val session = litert_lm_engine_create_session(enginePtr, sessionConfig)
            litert_lm_session_config_delete(sessionConfig)
            SessionHandle(session ?: throw LiteRtLmException("Failed to create session"))
        }

    private fun MemScope.samplerParamsPtr(sampler: SamplerConfig): CPointer<LiteRtLmSamplerParams> {
        val params = alloc<LiteRtLmSamplerParams>()
        params.type = kLiteRtLmSamplerTypeTopP
        params.top_k = sampler.topK
        params.top_p = sampler.topP.toFloat()
        params.temperature = sampler.temperature.toFloat()
        params.seed = sampler.seed
        return params.ptr
    }
}
