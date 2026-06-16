package com.phamtunglam.lamity.feature.chat.domain

import com.phamtunglam.lamity.feature.llmModels.domain.LlmBackend

/** A user-facing chat failure: either a known, localizable [kind][ChatErrorKind] or a raw native message. */
sealed interface ChatError {
    data class Known(val kind: ChatErrorKind) : ChatError

    data class Raw(val message: String) : ChatError
}

/** Known, localizable failure kinds derived from native LiteRT-LM errors. */
enum class ChatErrorKind {
    /** The model can't run on this device on any backend (GPU-only model, or the CPU fallback also failed). */
    MODEL_UNSUPPORTED_ON_DEVICE,

    /** The model failed to load on the GPU backend; switching it to CPU may help. */
    GPU_LOAD_FAILED,
}

/** A non-error, informational chat notice. */
enum class ChatNotice {
    /** A GPU load failed and the model was transparently switched to the CPU backend. */
    SWITCHED_TO_CPU,
}

/**
 * Substrings of native LiteRT-LM GPU / compiled-model-executor failures that a CPU retry is likely
 * to recover. Matched case-insensitively. Sources: google-ai-edge/LiteRT-LM #2461 (BATCH_MATMUL /
 * STABLEHLO_COMPOSITE prepare, RESOURCE_EXHAUSTED), #1850 (clEnqueueNDRangeKernel), gallery#557
 * (llm_litert_compiled_model_executor.cc).
 */
private val GPU_FAILURE_MARKERS =
    listOf(
        "llm_litert_compiled_model_executor",
        "resource_exhausted",
        "clenqueuendrangekernel",
        "litert_cl",
        "failed to prepare",
        "batch_matmul",
        "stablehlo_composite",
        "gpu",
        "delegate",
    )

/** Substring indicating the model itself forbids non-GPU execution, so a CPU retry can't help. */
private const val GPU_ONLY_MODEL_MARKER = "section_backend_constraint"

/** True when a GPU model load failed with a [message] that a CPU retry is likely to recover. */
fun isRecoverableGpuFailure(message: String?): Boolean {
    val text = message?.lowercase() ?: return false
    // A GPU-only-model rejection is not recoverable on CPU.
    if (GPU_ONLY_MODEL_MARKER in text) return false
    return GPU_FAILURE_MARKERS.any { it in text }
}

/** Whether a [backend] model whose load failed with [message] should be retried on the CPU backend. */
fun shouldFallBackToCpu(backend: LlmBackend, message: String?): Boolean =
    backend == LlmBackend.GPU && isRecoverableGpuFailure(message)

/**
 * Classifies an unrecoverable native load/generation error into a known, localizable
 * [ChatErrorKind], or null when it should be surfaced verbatim.
 */
fun classifyLoadError(message: String?): ChatErrorKind? {
    val text = message?.lowercase() ?: return null
    if (GPU_ONLY_MODEL_MARKER in text) return ChatErrorKind.MODEL_UNSUPPORTED_ON_DEVICE
    if (GPU_FAILURE_MARKERS.any { it in text }) return ChatErrorKind.GPU_LOAD_FAILED
    return null
}

/** Maps an unrecoverable load failure [message] to a user-facing [ChatError]. */
fun loadError(message: String?): ChatError =
    classifyLoadError(message)?.let { ChatError.Known(it) }
        ?: ChatError.Raw(message ?: "Could not load model")
