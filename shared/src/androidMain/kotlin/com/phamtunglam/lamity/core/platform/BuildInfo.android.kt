package com.phamtunglam.lamity.core.platform

/**
 * Reads the app module's generated BuildConfig reflectively — shared library
 * code cannot depend on :androidApp at compile time.
 */
actual fun currentBuildInfo(): BuildInfo = runCatching {
    val buildConfig = Class.forName("com.phamtunglam.lamity.BuildConfig")
    fun field(name: String): Any? = buildConfig.getField(name).get(null)
    BuildInfo(
        isDebug = field("DEBUG") as? Boolean ?: false,
        versionName = field("VERSION_NAME") as? String ?: "0",
        versionCode = (field("VERSION_CODE") as? Int)?.toLong() ?: 0L,
        bundleId = field("APPLICATION_ID") as? String ?: "com.phamtunglam.lamity",
    )
}.getOrDefault(
    BuildInfo(isDebug = false, versionName = "0", versionCode = 0, bundleId = "com.phamtunglam.lamity"),
)
