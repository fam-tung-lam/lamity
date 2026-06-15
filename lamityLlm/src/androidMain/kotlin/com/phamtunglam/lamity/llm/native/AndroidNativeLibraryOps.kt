@file:OptIn(ExperimentalApi::class)

package com.phamtunglam.lamity.llm.native

import com.google.ai.edge.litertlm.Engine as SdkEngine
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.LogSeverity as SdkLogSeverity
import com.google.ai.edge.litertlm.benchmark as sdkBenchmark
import com.phamtunglam.lamity.llm.model.BenchmarkInfo
import com.phamtunglam.lamity.llm.model.EngineConfig
import com.phamtunglam.lamity.llm.model.LogSeverity

internal actual fun nativeBenchmark(config: EngineConfig, prefillTokens: Int, decodeTokens: Int): BenchmarkInfo =
    sdkBenchmark(config.modelPath, config.backend.toSdk(), prefillTokens, decodeTokens, config.cacheDir).toCommon()

@Suppress("MagicNumber") // 0..5 map to the LiteRT-LM log-severity levels.
internal actual fun nativeSetMinLogLevel(severity: LogSeverity) {
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
