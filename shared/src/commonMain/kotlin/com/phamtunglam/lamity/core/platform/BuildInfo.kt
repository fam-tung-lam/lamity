package com.phamtunglam.lamity.core.platform

/** Build facts of the host app, read from platform metadata at runtime. */
data class BuildInfo(
    val isDebug: Boolean,
    val versionName: String,
    val versionCode: Long,
    val bundleId: String,
)

/**
 * Android reads the app's generated BuildConfig; iOS reads the main bundle's
 * Info.plist and the binary's debug flag.
 */
expect fun currentBuildInfo(): BuildInfo
