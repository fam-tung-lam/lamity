package com.phamtunglam.lamity.llm

import com.phamtunglam.lamity.llm.native.CapabilitiesHandle
import com.phamtunglam.lamity.llm.native.LiteRtLmNativeRuntime
import com.phamtunglam.lamity.llm.native.createNativeRuntime

/** Queries capabilities supported by the LiteRT-LM file at [modelPath]. */
class Capabilities(val modelPath: String) {
    private val runtime: LiteRtLmNativeRuntime = createNativeRuntime()
    private var handle: CapabilitiesHandle? = runtime.createCapabilities(modelPath)

    /** Whether these capabilities are still loaded. */
    val isAlive: Boolean get() = handle != null

    /** Whether the loaded LiteRT-LM file supports speculative decoding. */
    fun hasSpeculativeDecodingSupport(): Boolean {
        val current = handle ?: throw LiteRtLmException("Capabilities is already disposed")
        return runtime.hasSpeculativeDecodingSupport(current)
    }

    /** Releases the loaded capability resources. */
    fun dispose() {
        val current = handle ?: return
        handle = null
        runtime.deleteCapabilities(current)
    }
}
