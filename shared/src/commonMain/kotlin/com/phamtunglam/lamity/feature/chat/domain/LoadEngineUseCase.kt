package com.phamtunglam.lamity.feature.chat.domain

import com.phamtunglam.lamity.core.domain.platform.AppDirs
import com.phamtunglam.lamity.feature.llmModels.data.ModelFiles
import com.phamtunglam.lamity.feature.llmModels.domain.LlmBackend
import com.phamtunglam.lamity.feature.llmModels.domain.LlmModel
import com.phamtunglam.lamity.feature.llmModels.domain.ModelConfig
import com.phamtunglam.lamity.llm.model.Backend
import com.phamtunglam.lamity.llm.model.EngineConfig
import com.phamtunglam.lamity.logger.LamityLogger

/** Outcome of loading the native engine for a chat turn. */
sealed interface EngineLoad {
    /** Loaded with the requested config. */
    data class Ready(val config: ModelConfig) : EngineLoad

    /** A recoverable GPU failure was retried on CPU; [config] is the effective CPU config. */
    data class SwitchedToCpu(val config: ModelConfig) : EngineLoad

    /** The engine could not be loaded; [error] is user-facing. */
    data class Failed(val error: ChatError) : EngineLoad
}

private const val TAG = "LoadEngineUseCase"

/**
 * Loads the engine for [model] with the given config, transparently retrying once on the CPU backend
 * when a GPU load fails with a recoverable native error (see [shouldFallBackToCpu] — e.g. the Gemma 4
 * E-series `llm_litert_compiled_model_executor` failures). Idempotent: [ModelRuntime.ensureEngine]
 * dedupes by key, so warming an already-loaded engine is a no-op.
 */
class LoadEngineUseCase(
    private val runtime: ModelRuntime,
    private val modelFiles: ModelFiles,
    private val dirs: AppDirs,
) {
    suspend operator fun invoke(model: LlmModel, config: ModelConfig): EngineLoad {
        val first = ensureEngine(model, config)
        if (first.isSuccess) return EngineLoad.Ready(config)

        val cause = first.exceptionOrNull()
        if (!shouldFallBackToCpu(config.backend, cause?.message)) {
            return EngineLoad.Failed(loadError(cause?.message))
        }

        LamityLogger.w(TAG) { "GPU load failed for ${model.id}; retrying on CPU: ${cause?.message}" }
        val cpuConfig = config.copy(backend = LlmBackend.CPU)
        if (ensureEngine(model, cpuConfig).isFailure) {
            return EngineLoad.Failed(ChatError.Known(ChatErrorKind.MODEL_UNSUPPORTED_ON_DEVICE))
        }
        return EngineLoad.SwitchedToCpu(cpuConfig)
    }

    private suspend fun ensureEngine(model: LlmModel, config: ModelConfig): Result<Unit> {
        val engineKey = "${model.id}|${config.backend.name}|${config.maxTokens}"
        val engineConfig =
            EngineConfig(
                modelPath = modelFiles.modelPath(model),
                backend = if (config.backend == LlmBackend.GPU) Backend.Gpu() else Backend.Cpu(),
                maxNumTokens = config.maxTokens,
                cacheDir = dirs.cacheDir,
            )
        return runtime.ensureEngine(model.id, engineKey, engineConfig)
    }
}
