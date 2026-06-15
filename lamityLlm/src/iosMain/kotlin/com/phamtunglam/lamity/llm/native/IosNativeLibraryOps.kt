@file:OptIn(ExperimentalForeignApi::class)

package com.phamtunglam.lamity.llm.native

import cnames.structs.LiteRtLmEngine
import com.phamtunglam.lamity.llm.LiteRtLmException
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_config_create
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_config_delete
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_create
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_delete
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_get_benchmark_info
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_optional_args_create
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_optional_args_delete
import com.phamtunglam.lamity.llm.cinterop.litert_lm_conversation_send_message
import com.phamtunglam.lamity.llm.cinterop.litert_lm_engine_create
import com.phamtunglam.lamity.llm.cinterop.litert_lm_engine_delete
import com.phamtunglam.lamity.llm.cinterop.litert_lm_engine_settings_delete
import com.phamtunglam.lamity.llm.cinterop.litert_lm_engine_settings_enable_benchmark
import com.phamtunglam.lamity.llm.cinterop.litert_lm_engine_settings_set_num_decode_tokens
import com.phamtunglam.lamity.llm.cinterop.litert_lm_engine_settings_set_num_prefill_tokens
import com.phamtunglam.lamity.llm.cinterop.litert_lm_json_response_delete
import com.phamtunglam.lamity.llm.cinterop.litert_lm_set_min_log_level
import com.phamtunglam.lamity.llm.model.BenchmarkInfo
import com.phamtunglam.lamity.llm.model.EngineConfig
import com.phamtunglam.lamity.llm.model.LogSeverity
import com.phamtunglam.lamity.llm.model.Message
import com.phamtunglam.lamity.llm.serialization.toJsonString
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi

internal actual fun nativeBenchmark(config: EngineConfig, prefillTokens: Int, decodeTokens: Int): BenchmarkInfo {
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

internal actual fun nativeSetMinLogLevel(severity: LogSeverity) {
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
