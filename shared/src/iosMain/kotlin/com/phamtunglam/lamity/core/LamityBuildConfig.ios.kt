package com.phamtunglam.lamity.core

import platform.Foundation.NSBundle
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

/** iOS implementation of [LamityBuildConfig]. */
@Suppress("UndocumentedPublicProperty")
@OptIn(ExperimentalNativeApi::class)
actual object LamityBuildConfig {
    actual val buildType: BuildType
        get() = if (Platform.isDebugBinary) BuildType.DEBUG else BuildType.RELEASE
    actual val appVersion: String
        get() = infoPlistString("CFBundleShortVersionString").ifEmpty { "unknown" }
    actual val appVersionCode: Int
        get() = infoPlistString("CFBundleVersion").toIntOrNull() ?: 0
    actual val packageName: String
        get() = NSBundle.mainBundle.bundleIdentifier ?: ""
    actual val sentryDsn: String
        get() = infoPlistString("SENTRY_DSN")
    actual val hfToken: String
        get() = infoPlistString("HF_TOKEN")
}

private fun infoPlistString(key: String): String = NSBundle.mainBundle.objectForInfoDictionaryKey(key) as? String ?: ""
