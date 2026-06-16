package com.phamtunglam.lamity.llm

import com.phamtunglam.lamity.llm.native.CapabilitiesHandle
import com.phamtunglam.lamity.llm.native.CapabilityNativeRuntime
import com.phamtunglam.lamity.llm.native.createCapabilityNativeRuntime

/**
 * Queries capabilities supported by the LiteRT-LM file at [modelPath].
 *
 * The public constructor wires the real native runtime; the primary [internal] constructor takes an
 * injected [runtime] so tests can supply a fake.
 */
class Capabilities internal constructor(val modelPath: String, private val runtime: CapabilityNativeRuntime) {
    constructor(modelPath: String) : this(modelPath, createCapabilityNativeRuntime())

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
