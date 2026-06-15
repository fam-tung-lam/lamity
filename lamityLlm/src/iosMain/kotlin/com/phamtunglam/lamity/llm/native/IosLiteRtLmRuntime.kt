@file:OptIn(ExperimentalForeignApi::class)

package com.phamtunglam.lamity.llm.native

import cnames.structs.LiteRtLmConversation
import cnames.structs.LiteRtLmEngine
import com.phamtunglam.lamity.llm.LiteRtLmException
import com.phamtunglam.lamity.llm.cinterop.LiteRtLmSamplerParams
import com.phamtunglam.lamity.llm.cinterop.kLiteRtLmSamplerTypeTopP
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_cancel_process
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_config_create
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_config_delete
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_config_set_enable_constrained_decoding
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_config_set_messages
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_config_set_session_config
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_config_set_system_message
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_config_set_tools
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_create
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_delete
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_get_token_count
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_optional_args_create
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_optional_args_delete
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_render_message_to_string
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_send_message_stream
import com.phamtunglam.lamity.llm.cinterop.litert_lm_engine_create
import com.phamtunglam.lamity.llm.cinterop.litert_lm_engine_delete
import com.phamtunglam.lamity.llm.cinterop.litert_lm_engine_settings_create
import com.phamtunglam.lamity.llm.cinterop.litert_lm_engine_settings_delete
import com.phamtunglam.lamity.llm.cinterop.litert_lm_engine_settings_set_cache_dir
import com.phamtunglam.lamity.llm.cinterop.litert_lm_engine_settings_set_max_num_tokens
import com.phamtunglam.lamity.llm.cinterop.litert_lm_session_config_create
import com.phamtunglam.lamity.llm.cinterop.litert_lm_session_config_delete
import com.phamtunglam.lamity.llm.cinterop.litert_lm_session_config_set_sampler_params
import com.phamtunglam.lamity.llm.cinterop.litert_lm_set_min_log_level
import com.phamtunglam.lamity.llm.model.EngineConfig
import com.phamtunglam.lamity.llm.model.Message
import com.phamtunglam.lamity.llm.model.SamplerConfig
import com.phamtunglam.lamity.llm.serialization.toJsonArrayString
import com.phamtunglam.lamity.llm.serialization.toJsonString
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString
import kotlinx.serialization.json.JsonObject

internal actual fun createNativeRuntime(): LiteRtLmNativeRuntime = IosLiteRtLmRuntime()

/**
 * [LiteRtLmNativeRuntime] over the `CLiteRTLM` C API via Kotlin/Native cinterop.
 * JSON is the wire format; tool calls are surfaced (never executed natively) and
 * the common [com.phamtunglam.lamity.llm.Conversation] runs the tool loop.
 */
@Suppress("UNCHECKED_CAST")
internal class IosLiteRtLmRuntime : LiteRtLmNativeRuntime {
    override fun createEngine(config: EngineConfig): EngineHandle {
        val settings =
            litert_lm_engine_settings_create(
                config.modelPath,
                config.backend.name,
                config.visionBackend?.name,
                config.audioBackend?.name,
            ) ?: throw LiteRtLmException("Failed to create engine settings")
        config.maxNumTokens?.let { litert_lm_engine_settings_set_max_num_tokens(settings, it) }
        config.cacheDir?.let { litert_lm_engine_settings_set_cache_dir(settings, it) }
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
        toolsJson: String,
    ): ConversationHandle =
        memScoped {
            val enginePtr = engine.native as CPointer<LiteRtLmEngine>
            val sessionConfig =
                litert_lm_session_config_create()
                    ?: throw LiteRtLmException("Failed to create session config")
            samplerConfig?.let { sampler ->
                val params = alloc<LiteRtLmSamplerParams>()
                params.type = kLiteRtLmSamplerTypeTopP
                params.top_k = sampler.topK
                params.top_p = sampler.topP.toFloat()
                params.temperature = sampler.temperature.toFloat()
                params.seed = sampler.seed
                litert_lm_session_config_set_sampler_params(sessionConfig, params.ptr)
            }

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
            litert_lm_conversation_config_set_enable_constrained_decoding(convConfig, false)

            val conversation = litert_lm_conversation_create(enginePtr, convConfig)
            litert_lm_conversation_config_delete(convConfig)
            litert_lm_session_config_delete(sessionConfig)
            ConversationHandle(conversation ?: throw LiteRtLmException("Failed to create conversation"))
        }

    override fun deleteConversation(handle: ConversationHandle) {
        litert_lm_conversation_delete(handle.native as CPointer<LiteRtLmConversation>)
    }

    override fun streamTurn(
        conversation: ConversationHandle,
        message: Message,
        extraContext: JsonObject?,
        callback: TurnCallback,
    ) {
        val conversationPtr = conversation.native as CPointer<LiteRtLmConversation>
        val ref = StableRef.create(IosTurnContext(callback))
        val optionalArgs = litert_lm_conversation_optional_args_create()
        val status =
            litert_lm_conversation_send_message_stream(
                conversationPtr,
                message.toJsonString(),
                extraContext?.toString(),
                optionalArgs,
                staticCFunction(::iosStreamCallback),
                ref.asCPointer(),
            )
        litert_lm_conversation_optional_args_delete(optionalArgs)
        if (status != 0) {
            ref.dispose()
            callback.onError("Failed to start stream (status $status)")
        }
    }

    override fun cancel(conversation: ConversationHandle) {
        litert_lm_conversation_cancel_process(conversation.native as CPointer<LiteRtLmConversation>)
    }

    override fun getTokenCount(conversation: ConversationHandle): Int =
        litert_lm_conversation_get_token_count(conversation.native as CPointer<LiteRtLmConversation>)

    override fun renderMessage(conversation: ConversationHandle, message: Message): String =
        litert_lm_conversation_render_message_to_string(
            conversation.native as CPointer<LiteRtLmConversation>,
            message.toJsonString(),
        )?.toKString() ?: throw LiteRtLmException("Failed to render message")

    override fun setMinLogLevel(level: Int) {
        litert_lm_set_min_log_level(level)
    }
}
