@file:OptIn(ExperimentalApi::class)

package com.phamtunglam.lamity.llm.native

import com.google.ai.edge.litertlm.Capabilities as SdkCapabilities
import com.google.ai.edge.litertlm.ExperimentalApi

internal actual fun createCapabilityNativeRuntime(): CapabilityNativeRuntime = AndroidCapabilityRuntime()

/** [CapabilityNativeRuntime] over the `com.google.ai.edge.litertlm` Kotlin SDK. */
internal class AndroidCapabilityRuntime : CapabilityNativeRuntime {
    override fun createCapabilities(modelPath: String): CapabilitiesHandle =
        CapabilitiesHandle(SdkCapabilities(modelPath))

    override fun deleteCapabilities(handle: CapabilitiesHandle) {
        (handle.native as SdkCapabilities).close()
    }

    override fun hasSpeculativeDecodingSupport(handle: CapabilitiesHandle): Boolean =
        (handle.native as SdkCapabilities).hasSpeculativeDecodingSupport()
}
