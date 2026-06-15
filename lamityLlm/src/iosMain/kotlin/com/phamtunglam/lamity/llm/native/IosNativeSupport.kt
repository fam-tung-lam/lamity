package com.phamtunglam.lamity.llm.native

import cnames.structs.LiteRtLmBenchmarkInfo
import cnames.structs.LiteRtLmEngineSettings
import com.phamtunglam.lamity.llm.LiteRtLmException
import com.phamtunglam.lamity.llm.cinterop.litert_lm_benchmark_info_delete
import com.phamtunglam.lamity.llm.cinterop.litert_lm_benchmark_info_get_decode_token_count_at
import com.phamtunglam.lamity.llm.cinterop.litert_lm_benchmark_info_get_decode_tokens_per_sec_at
import com.phamtunglam.lamity.llm.cinterop.litert_lm_benchmark_info_get_num_decode_turns
import com.phamtunglam.lamity.llm.cinterop.litert_lm_benchmark_info_get_num_prefill_turns
import com.phamtunglam.lamity.llm.cinterop.litert_lm_benchmark_info_get_prefill_token_count_at
import com.phamtunglam.lamity.llm.cinterop.litert_lm_benchmark_info_get_prefill_tokens_per_sec_at
import com.phamtunglam.lamity.llm.cinterop.litert_lm_benchmark_info_get_time_to_first_token
import com.phamtunglam.lamity.llm.cinterop.litert_lm_benchmark_info_get_total_init_time_in_second
import com.phamtunglam.lamity.llm.cinterop.litert_lm_engine_settings_create
import com.phamtunglam.lamity.llm.cinterop.litert_lm_engine_settings_set_cache_dir
import com.phamtunglam.lamity.llm.cinterop.litert_lm_engine_settings_set_litert_dispatch_lib_dir
import com.phamtunglam.lamity.llm.cinterop.litert_lm_engine_settings_set_max_num_images
import com.phamtunglam.lamity.llm.cinterop.litert_lm_engine_settings_set_max_num_tokens
import com.phamtunglam.lamity.llm.model.Backend
import com.phamtunglam.lamity.llm.model.BenchmarkInfo
import com.phamtunglam.lamity.llm.model.EngineConfig
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi

/** Builds native engine settings from [config]; the caller owns and must delete the result. */
@OptIn(ExperimentalForeignApi::class)
internal fun buildEngineSettings(config: EngineConfig): CPointer<LiteRtLmEngineSettings> {
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

/** Reads a [BenchmarkInfo] from the native handle, deleting the handle before returning. */
@OptIn(ExperimentalForeignApi::class)
@Suppress("MagicNumber") // 0/0.0 fallbacks when no benchmark turns were recorded.
internal fun readBenchmarkInfo(info: CPointer<LiteRtLmBenchmarkInfo>): BenchmarkInfo {
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
