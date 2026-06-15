package com.phamtunglam.lamity.llm.model

/** Benchmark information for an engine, conversation, or session. */
data class BenchmarkInfo(
    val initTimeInSecond: Double,
    val timeToFirstTokenInSecond: Double,
    val lastPrefillTokenCount: Int,
    val lastDecodeTokenCount: Int,
    val lastPrefillTokensPerSecond: Double,
    val lastDecodeTokensPerSecond: Double,
)
