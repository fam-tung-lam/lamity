package com.phamtunglam.lamity.llm.native

/** Opaque platform capabilities handle (Android `Capabilities` / iOS C loaded-file pointer). */
internal class CapabilitiesHandle(val native: Any)

internal expect fun createCapabilityNativeRuntime(): CapabilityNativeRuntime

/**
 * Loads a LiteRT-LM file to query the capabilities it supports.
 */
internal interface CapabilityNativeRuntime {
    fun createCapabilities(modelPath: String): CapabilitiesHandle

    fun deleteCapabilities(handle: CapabilitiesHandle)

    fun hasSpeculativeDecodingSupport(handle: CapabilitiesHandle): Boolean
}
