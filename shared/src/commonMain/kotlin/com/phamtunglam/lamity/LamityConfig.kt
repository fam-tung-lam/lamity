package com.phamtunglam.lamity

import com.phamtunglam.lamity.core.platform.currentBuildInfo
import com.phamtunglam.lamity.crashreporter.models.LamityCrashReporterConfig

object LamityConfig {

    /**
     * Sentry DSN for crash reporting. Blank disables reporting (all crash
     * reporter calls become no-ops). A DSN only identifies the ingestion
     * endpoint — it is not a secret — but rotate it if the project changes
     * hands.
     */
    const val SENTRY_DSN = "YOUR_SENTRY_DSN"

    fun lamityCrashReporterConfig(): LamityCrashReporterConfig {
        val build = currentBuildInfo()
        return LamityCrashReporterConfig(
            dsn = SENTRY_DSN,
            environment = if (build.isDebug) "development" else "production",
            release = "${build.bundleId}@${build.versionName}+${build.versionCode}",
            debug = build.isDebug,
        )
    }
}
