package com.phamtunglam.lamity.llm

import com.phamtunglam.lamity.llm.model.BenchmarkInfo
import com.phamtunglam.lamity.llm.model.EngineConfig
import com.phamtunglam.lamity.llm.model.LogSeverity
import com.phamtunglam.lamity.llm.native.createNativeRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val DEFAULT_BENCHMARK_TOKENS = 256

/** Sets the minimum log level for the native LiteRT-LM library. */
fun setMinimumLogLevel(severity: LogSeverity) {
    createNativeRuntime().setMinLogLevel(severity)
}

/**
 * Runs a LiteRT-LM benchmark for the model described by [config], using [prefillTokens] and
 * [decodeTokens] synthetic tokens. Heavy (loads the model); runs off the main thread.
 */
suspend fun benchmark(
    config: EngineConfig,
    prefillTokens: Int = DEFAULT_BENCHMARK_TOKENS,
    decodeTokens: Int = DEFAULT_BENCHMARK_TOKENS,
): BenchmarkInfo {
    require(prefillTokens > 0) { "prefillTokens must be positive" }
    require(decodeTokens > 0) { "decodeTokens must be positive" }
    return withContext(Dispatchers.Default) {
        createNativeRuntime().benchmark(config, prefillTokens, decodeTokens)
    }
}
