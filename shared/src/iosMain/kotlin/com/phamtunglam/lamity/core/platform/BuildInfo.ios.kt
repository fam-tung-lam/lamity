package com.phamtunglam.lamity.core.platform

import kotlin.experimental.ExperimentalNativeApi
import platform.Foundation.NSBundle

@OptIn(ExperimentalNativeApi::class)
actual fun currentBuildInfo(): BuildInfo {
    val info = NSBundle.mainBundle.infoDictionary
    return BuildInfo(
        isDebug = Platform.isDebugBinary,
        versionName = info?.get("CFBundleShortVersionString") as? String ?: "0",
        versionCode = (info?.get("CFBundleVersion") as? String)?.toLongOrNull() ?: 0L,
        bundleId = NSBundle.mainBundle.bundleIdentifier ?: "com.phamtunglam.lamity",
    )
}
