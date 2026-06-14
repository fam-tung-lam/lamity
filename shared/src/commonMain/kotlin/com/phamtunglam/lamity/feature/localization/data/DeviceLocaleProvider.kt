package com.phamtunglam.lamity.feature.localization.data

/** Provides the current device locale as a BCP 47 tag. */
fun interface DeviceLocaleProvider {
    /** Returns the current device locale tag, or null when unavailable. */
    fun currentLocaleTag(): String?
}

/** Returns the platform-backed [DeviceLocaleProvider]. */
expect fun systemDeviceLocaleProvider(): DeviceLocaleProvider
