package com.phamtunglam.lamity.llm.native

import com.google.ai.edge.litertlm.BenchmarkInfo as SdkBenchmarkInfo
import com.google.ai.edge.litertlm.InputData as SdkInputData
import com.google.ai.edge.litertlm.LoraConfig as SdkLoraConfig
import com.phamtunglam.lamity.llm.model.BenchmarkInfo
import com.phamtunglam.lamity.llm.model.InputData
import com.phamtunglam.lamity.llm.model.LoraConfig

internal fun InputData.toSdk(): SdkInputData =
    when (this) {
        is InputData.Text -> SdkInputData.Text(text)
        is InputData.ImageBytes -> SdkInputData.Image(bytes)
        is InputData.AudioBytes -> SdkInputData.Audio(bytes)
    }

internal fun LoraConfig.toSdk(): SdkLoraConfig = SdkLoraConfig(loraPath, audioLoraPath)

internal fun SdkBenchmarkInfo.toCommon(): BenchmarkInfo =
    BenchmarkInfo(
        initTimeInSecond = initTimeInSecond,
        timeToFirstTokenInSecond = timeToFirstTokenInSecond,
        lastPrefillTokenCount = lastPrefillTokenCount,
        lastDecodeTokenCount = lastDecodeTokenCount,
        lastPrefillTokensPerSecond = lastPrefillTokensPerSecond,
        lastDecodeTokensPerSecond = lastDecodeTokensPerSecond,
    )
