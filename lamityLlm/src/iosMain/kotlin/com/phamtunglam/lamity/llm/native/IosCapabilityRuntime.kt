@file:OptIn(ExperimentalForeignApi::class)

package com.phamtunglam.lamity.llm.native

import cnames.structs.LiteRtLmLoadedFile
import com.phamtunglam.lamity.llm.LiteRtLmException
import com.phamtunglam.lamity.llm.cinterop.litert_lm_loaded_file_create
import com.phamtunglam.lamity.llm.cinterop.litert_lm_loaded_file_delete
import com.phamtunglam.lamity.llm.cinterop.litert_lm_loaded_file_has_speculative_decoding_support
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi

internal actual fun createCapabilityNativeRuntime(): CapabilityNativeRuntime = IosCapabilityRuntime()

/** [CapabilityNativeRuntime] over the `CLiteRTLM` C API via Kotlin/Native cinterop. */
@Suppress("UNCHECKED_CAST")
internal class IosCapabilityRuntime : CapabilityNativeRuntime {
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
}
