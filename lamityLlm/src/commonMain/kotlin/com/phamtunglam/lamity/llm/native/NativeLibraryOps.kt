package com.phamtunglam.lamity.llm.native

import com.phamtunglam.lamity.llm.model.BenchmarkInfo
import com.phamtunglam.lamity.llm.model.EngineConfig
import com.phamtunglam.lamity.llm.model.LogSeverity

/** Runs a native LiteRT-LM benchmark for the model described by [config]. */
internal expect fun nativeBenchmark(config: EngineConfig, prefillTokens: Int, decodeTokens: Int): BenchmarkInfo

/** Sets the minimum log level for the native LiteRT-LM library. */
internal expect fun nativeSetMinLogLevel(severity: LogSeverity)
