package com.phamtunglam.lamity.core

/** App build variants. */
enum class BuildType(
    /** Deployment environment name reported with crashes, e.g. `development` or `production`. */
    val environmentName: String,
) {
    /** Debug build. */
    DEBUG("development"),

    /** Release build. */
    RELEASE("production"),
}

/** Exposes platform build metadata used by shared code. */
expect object LamityBuildConfig {
    /** Build variant. */
    val buildType: BuildType

    /** App marketing version. */
    val appVersion: String

    /** App build number. */
    val appVersionCode: Int

    /** App package name or bundle identifier. */
    val packageName: String

    /** Sentry DSN from local platform configuration. Blank disables crash reporting. */
    val sentryDsn: String

    /** Hugging Face token from local platform configuration. Blank disables authenticated downloads. */
    val hfToken: String
}
