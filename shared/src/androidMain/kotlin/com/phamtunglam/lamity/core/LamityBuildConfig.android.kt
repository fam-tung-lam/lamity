package com.phamtunglam.lamity.core

/** Android implementation of [LamityBuildConfig]. */
@Suppress("UndocumentedPublicProperty")
actual object LamityBuildConfig {
    actual val buildType: BuildType
        get() = if (appBuildConfigBoolean("DEBUG")) BuildType.DEBUG else BuildType.RELEASE
    actual val appVersion: String
        get() = appBuildConfigString("VERSION_NAME")
    actual val appVersionCode: Int
        get() = appBuildConfigInt("VERSION_CODE")
    actual val packageName: String
        get() = appBuildConfigString("APPLICATION_ID")
    actual val sentryDsn: String
        get() = appBuildConfigString("SENTRY_DSN", defaultValue = "")
    actual val hfToken: String
        get() = appBuildConfigString("HF_TOKEN", defaultValue = "")
}

private const val ANDROID_APP_BUILD_CONFIG_CLASS_NAME = "com.phamtunglam.lamity.BuildConfig"

private fun appBuildConfigBoolean(name: String): Boolean = appBuildConfigField(name) as? Boolean ?: false

private fun appBuildConfigString(name: String, defaultValue: String = "unknown"): String =
    appBuildConfigField(name) as? String ?: defaultValue

private fun appBuildConfigInt(name: String): Int = appBuildConfigField(name) as? Int ?: 0

// Shared library code cannot depend on :androidApp at compile time, so the app
// module's generated BuildConfig is read reflectively.
private fun appBuildConfigField(name: String): Any? =
    runCatching {
        Class.forName(ANDROID_APP_BUILD_CONFIG_CLASS_NAME).getField(name).get(null)
    }.getOrNull()
