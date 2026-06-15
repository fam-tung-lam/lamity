@file:OptIn(ExperimentalForeignApi::class)
@file:Suppress("TooManyFunctions")

package com.phamtunglam.lamity.llm.native

import cnames.structs.LiteRtLmBenchmarkInfo
import cnames.structs.LiteRtLmConversation
import cnames.structs.LiteRtLmEngine
import cnames.structs.LiteRtLmEngineSettings
import cnames.structs.LiteRtLmLoadedFile
import cnames.structs.LiteRtLmResponses
import cnames.structs.LiteRtLmSession
import com.phamtunglam.lamity.llm.LiteRtLmException
import com.phamtunglam.lamity.llm.cinterop.LiteRtLmInputData
import com.phamtunglam.lamity.llm.cinterop.LiteRtLmInputDataType.kLiteRtLmInputDataTypeAudio
import com.phamtunglam.lamity.llm.cinterop.LiteRtLmInputDataType.kLiteRtLmInputDataTypeImage
import com.phamtunglam.lamity.llm.cinterop.LiteRtLmInputDataType.kLiteRtLmInputDataTypeText
import com.phamtunglam.lamity.llm.cinterop.LiteRtLmSamplerParams
import com.phamtunglam.lamity.llm.cinterop.kLiteRtLmSamplerTypeTopP
import com.phamtunglam.lamity.llm.cinterop.litert_lm_benchmark_info_delete
import com.phamtunglam.lamity.llm.cinterop.litert_lm_benchmark_info_get_decode_token_count_at
import com.phamtunglam.lamity.llm.cinterop.litert_lm_benchmark_info_get_decode_tokens_per_sec_at
import com.phamtunglam.lamity.llm.cinterop.litert_lm_benchmark_info_get_num_decode_turns
import com.phamtunglam.lamity.llm.cinterop.litert_lm_benchmark_info_get_num_prefill_turns
import com.phamtunglam.lamity.llm.cinterop.litert_lm_benchmark_info_get_prefill_token_count_at
import com.phamtunglam.lamity.llm.cinterop.litert_lm_benchmark_info_get_prefill_tokens_per_sec_at
import com.phamtunglam.lamity.llm.cinterop.litert_lm_benchmark_info_get_time_to_first_token
import com.phamtunglam.lamity.llm.cinterop.litert_lm_benchmark_info_get_total_init_time_in_second
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_cancel_process
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_config_create
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_config_delete
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_config_set_enable_constrained_decoding
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_config_set_extra_context
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_config_set_messages
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_config_set_session_config
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_config_set_system_message
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_config_set_tools
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_create
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_delete
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_get_benchmark_info
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_get_token_count
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_optional_args_create
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_optional_args_delete
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_render_message_to_string
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_send_message
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_send_message_stream
import com.phamtunglam.lamity.llm.cinterop.litert_lm_engine_create
import com.phamtunglam.lamity.llm.cinterop.litert_lm_engine_create_session
import com.phamtunglam.lamity.llm.cinterop.litert_lm_engine_delete
import com.phamtunglam.lamity.llm.cinterop.litert_lm_engine_settings_create
import com.phamtunglam.lamity.llm.cinterop.litert_lm_engine_settings_delete
import com.phamtunglam.lamity.llm.cinterop.litert_lm_engine_settings_enable_benchmark
import com.phamtunglam.lamity.llm.cinterop.litert_lm_engine_settings_set_cache_dir
import com.phamtunglam.lamity.llm.cinterop.litert_lm_engine_settings_set_litert_dispatch_lib_dir
import com.phamtunglam.lamity.llm.cinterop.litert_lm_engine_settings_set_max_num_images
import com.phamtunglam.lamity.llm.cinterop.litert_lm_engine_settings_set_max_num_tokens
import com.phamtunglam.lamity.llm.cinterop.litert_lm_engine_settings_set_num_decode_tokens
import com.phamtunglam.lamity.llm.cinterop.litert_lm_engine_settings_set_num_prefill_tokens
import com.phamtunglam.lamity.llm.cinterop.litert_lm_json_response_delete
import com.phamtunglam.lamity.llm.cinterop.litert_lm_loaded_file_create
import com.phamtunglam.lamity.llm.cinterop.litert_lm_loaded_file_delete
import com.phamtunglam.lamity.llm.cinterop.litert_lm_loaded_file_has_speculative_decoding_support
import com.phamtunglam.lamity.llm.cinterop.litert_lm_responses_delete
import com.phamtunglam.lamity.llm.cinterop.litert_lm_responses_get_num_candidates
import com.phamtunglam.lamity.llm.cinterop.litert_lm_responses_get_response_text_at
import com.phamtunglam.lamity.llm.cinterop.litert_lm_session_cancel_process
import com.phamtunglam.lamity.llm.cinterop.litert_lm_session_config_create
import com.phamtunglam.lamity.llm.cinterop.litert_lm_session_config_delete
import com.phamtunglam.lamity.llm.cinterop.litert_lm_session_config_set_audio_lora_path
import com.phamtunglam.lamity.llm.cinterop.litert_lm_session_config_set_lora_path
import com.phamtunglam.lamity.llm.cinterop.litert_lm_session_config_set_sampler_params
import com.phamtunglam.lamity.llm.cinterop.litert_lm_session_delete
import com.phamtunglam.lamity.llm.cinterop.litert_lm_session_generate_content
import com.phamtunglam.lamity.llm.cinterop.litert_lm_session_generate_content_stream
import com.phamtunglam.lamity.llm.cinterop.litert_lm_session_run_decode
import com.phamtunglam.lamity.llm.cinterop.litert_lm_session_run_prefill
import com.phamtunglam.lamity.llm.cinterop.litert_lm_set_min_log_level
import com.phamtunglam.lamity.llm.model.Backend
import com.phamtunglam.lamity.llm.model.BenchmarkInfo
import com.phamtunglam.lamity.llm.model.EngineConfig
import com.phamtunglam.lamity.llm.model.InputData
import com.phamtunglam.lamity.llm.model.LogSeverity
import com.phamtunglam.lamity.llm.model.LoraConfig
import com.phamtunglam.lamity.llm.model.Message
import com.phamtunglam.lamity.llm.model.SamplerConfig
import com.phamtunglam.lamity.llm.serialization.toJsonArrayString
import com.phamtunglam.lamity.llm.serialization.toJsonString
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import kotlinx.serialization.json.JsonObject
import platform.posix.memcpy

internal actual fun createNativeRuntime(): LiteRtLmNativeRuntime = IosLiteRtLmRuntime()

/**
 * [LiteRtLmNativeRuntime] over the `CLiteRTLM` C API via Kotlin/Native cinterop.
 * JSON is the wire format; tool calls are surfaced (never executed natively) and
 * the common [com.phamtunglam.lamity.llm.Conversation] runs the tool loop.
 */
@Suppress("UNCHECKED_CAST")
internal class IosLiteRtLmRuntime : LiteRtLmNativeRuntime {
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

    override fun getBenchmarkInfo(conversation: ConversationHandle): BenchmarkInfo {
        val info =
            litert_lm_conversation_get_benchmark_info(conversation.native as CPointer<LiteRtLmConversation>)
                ?: throw LiteRtLmException("Could not get benchmark info")
        return readBenchmarkInfo(info)
    }

    override fun renderMessage(conversation: ConversationHandle, message: Message): String =
        litert_lm_conversation_render_message_to_string(
            conversation.native as CPointer<LiteRtLmConversation>,
            message.toJsonString(),
        )?.toKString() ?: throw LiteRtLmException("Failed to render message")

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

    override fun deleteSession(handle: SessionHandle) {
        litert_lm_session_delete(handle.native as CPointer<LiteRtLmSession>)
    }

    override fun runSessionPrefill(session: SessionHandle, inputData: List<InputData>) {
        val sessionPtr = session.native as CPointer<LiteRtLmSession>
        val status =
            memScoped {
                withInputData(
                    inputData,
                ) { inputs, count -> litert_lm_session_run_prefill(sessionPtr, inputs, count) }
            }
        if (status != 0) throw LiteRtLmException("Session prefill failed (status $status)")
    }

    override fun runSessionDecode(session: SessionHandle): String =
        responsesFirstText(litert_lm_session_run_decode(session.native as CPointer<LiteRtLmSession>))

    override fun generateSessionContent(session: SessionHandle, inputData: List<InputData>): String {
        val sessionPtr = session.native as CPointer<LiteRtLmSession>
        val responses =
            memScoped {
                withInputData(
                    inputData,
                ) { inputs, count -> litert_lm_session_generate_content(sessionPtr, inputs, count) }
            }
        return responsesFirstText(responses)
    }

    override fun generateSessionContentStream(
        session: SessionHandle,
        inputData: List<InputData>,
        callback: SessionStreamCallback,
    ) {
        val sessionPtr = session.native as CPointer<LiteRtLmSession>
        val ref = StableRef.create(IosSessionContext(callback))
        val status =
            memScoped {
                withInputData(inputData) { inputs, count ->
                    litert_lm_session_generate_content_stream(
                        sessionPtr,
                        inputs,
                        count,
                        staticCFunction(::iosSessionStreamCallback),
                        ref.asCPointer(),
                    )
                }
            }
        if (status != 0) {
            ref.dispose()
            callback.onError("Failed to start session stream (status $status)")
        }
    }

    override fun cancelSession(session: SessionHandle) {
        litert_lm_session_cancel_process(session.native as CPointer<LiteRtLmSession>)
    }

    override fun createCapabilities(modelPath: String): CapabilitiesHandle {
        val loaded =
            litert_lm_loaded_file_create(modelPath)
                ?: throw LiteRtLmException("Could not load file for capabilities: $modelPath")
        return CapabilitiesHandle(loaded)
    }

    override fun deleteCapabilities(handle: CapabilitiesHandle) {
        litert_lm_loaded_file_delete(handle.native as CPointer<LiteRtLmLoadedFile>)
    }

    override fun hasSpeculativeDecodingSupport(handle: CapabilitiesHandle): Boolean =
        litert_lm_loaded_file_has_speculative_decoding_support(handle.native as CPointer<LiteRtLmLoadedFile>)

    override fun benchmark(config: EngineConfig, prefillTokens: Int, decodeTokens: Int): BenchmarkInfo {
        val settings = buildEngineSettings(config)
        litert_lm_engine_settings_enable_benchmark(settings)
        litert_lm_engine_settings_set_num_prefill_tokens(settings, prefillTokens)
        litert_lm_engine_settings_set_num_decode_tokens(settings, decodeTokens)
        val engine = litert_lm_engine_create(settings)
        litert_lm_engine_settings_delete(settings)
        val enginePtr = engine ?: throw LiteRtLmException("Failed to create engine for benchmark")
        try {
            return benchmarkOnEngine(enginePtr)
        } finally {
            litert_lm_engine_delete(enginePtr)
        }
    }

    override fun setMinLogLevel(severity: LogSeverity) {
        litert_lm_set_min_log_level(severity.level)
    }

    @Suppress("ThrowsCount") // Each native step can fail independently and must surface a clear error.
    private fun benchmarkOnEngine(enginePtr: CPointer<LiteRtLmEngine>): BenchmarkInfo {
        val convConfig =
            litert_lm_conversation_config_create()
                ?: throw LiteRtLmException("Failed to create conversation config")
        val conversation = litert_lm_conversation_create(enginePtr, convConfig)
        litert_lm_conversation_config_delete(convConfig)
        val conv = conversation ?: throw LiteRtLmException("Failed to create benchmark conversation")
        try {
            val optionalArgs = litert_lm_conversation_optional_args_create()
            val response =
                litert_lm_conversation_send_message(
                    conv,
                    Message.user("Engine ignore this message in this mode.").toJsonString(),
                    null,
                    optionalArgs,
                )
            litert_lm_conversation_optional_args_delete(optionalArgs)
            response?.let { litert_lm_json_response_delete(it) }
            val info =
                litert_lm_conversation_get_benchmark_info(conv)
                    ?: throw LiteRtLmException("Could not get benchmark info")
            return readBenchmarkInfo(info)
        } finally {
            litert_lm_conversation_delete(conv)
        }
    }

    private fun buildEngineSettings(config: EngineConfig): CPointer<LiteRtLmEngineSettings> {
        val settings =
            litert_lm_engine_settings_create(
                config.modelPath,
                config.backend.name,
                config.visionBackend?.name,
                config.audioBackend?.name,
            ) ?: throw LiteRtLmException("Failed to create engine settings")
        config.maxNumTokens?.let { litert_lm_engine_settings_set_max_num_tokens(settings, it) }
        config.maxNumImages?.let { litert_lm_engine_settings_set_max_num_images(settings, it) }
        config.cacheDir?.let { litert_lm_engine_settings_set_cache_dir(settings, it) }
        (config.backend as? Backend.Npu)?.nativeLibraryDir?.let {
            litert_lm_engine_settings_set_litert_dispatch_lib_dir(settings, it)
        }
        return settings
    }

    @Suppress("MagicNumber") // 0/0.0 fallbacks when no benchmark turns were recorded.
    private fun readBenchmarkInfo(info: CPointer<LiteRtLmBenchmarkInfo>): BenchmarkInfo {
        try {
            val numPrefill = litert_lm_benchmark_info_get_num_prefill_turns(info)
            val numDecode = litert_lm_benchmark_info_get_num_decode_turns(info)
            return BenchmarkInfo(
                initTimeInSecond = litert_lm_benchmark_info_get_total_init_time_in_second(info),
                timeToFirstTokenInSecond = litert_lm_benchmark_info_get_time_to_first_token(info),
                lastPrefillTokenCount =
                    if (numPrefill >
                        0
                    ) {
                        litert_lm_benchmark_info_get_prefill_token_count_at(info, numPrefill - 1)
                    } else {
                        0
                    },
                lastDecodeTokenCount =
                    if (numDecode > 0) litert_lm_benchmark_info_get_decode_token_count_at(info, numDecode - 1) else 0,
                lastPrefillTokensPerSecond =
                    if (numPrefill > 0) {
                        litert_lm_benchmark_info_get_prefill_tokens_per_sec_at(info, numPrefill - 1)
                    } else {
                        0.0
                    },
                lastDecodeTokensPerSecond =
                    if (numDecode > 0) {
                        litert_lm_benchmark_info_get_decode_tokens_per_sec_at(info, numDecode - 1)
                    } else {
                        0.0
                    },
            )
        } finally {
            litert_lm_benchmark_info_delete(info)
        }
    }

    private fun responsesFirstText(responses: CPointer<LiteRtLmResponses>?): String {
        if (responses == null) throw LiteRtLmException("LiteRT-LM returned no responses")
        try {
            if (litert_lm_responses_get_num_candidates(responses) <= 0) return ""
            return litert_lm_responses_get_response_text_at(responses, 0)?.toKString() ?: ""
        } finally {
            litert_lm_responses_delete(responses)
        }
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

    private fun <T> MemScope.withInputData(
        inputData: List<InputData>,
        block: (CValuesRef<LiteRtLmInputData>?, ULong) -> T,
    ): T {
        if (inputData.isEmpty()) return block(null, 0uL)
        val array = allocArray<LiteRtLmInputData>(inputData.size)
        inputData.forEachIndexed { index, input -> fillInputData(array[index], input) }
        return block(array, inputData.size.convert())
    }

    private fun MemScope.fillInputData(slot: LiteRtLmInputData, input: InputData) {
        val (type, bytes) =
            when (input) {
                is InputData.Text -> kLiteRtLmInputDataTypeText to input.text.encodeToByteArray()
                is InputData.ImageBytes -> kLiteRtLmInputDataTypeImage to input.bytes
                is InputData.AudioBytes -> kLiteRtLmInputDataTypeAudio to input.bytes
            }
        slot.type = type
        slot.size = bytes.size.convert()
        val buffer = allocArray<ByteVar>(if (bytes.isEmpty()) 1 else bytes.size)
        if (bytes.isNotEmpty()) {
            bytes.usePinned { pinned -> memcpy(buffer, pinned.addressOf(0), bytes.size.convert()) }
        }
        slot.data = buffer
    }
}
