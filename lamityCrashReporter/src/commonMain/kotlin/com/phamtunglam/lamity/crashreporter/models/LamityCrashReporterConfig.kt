package com.phamtunglam.lamity.crashreporter.models

import com.phamtunglam.lamity.crashreporter.LamityCrashReporter

/**
 * Configuration accepted by [LamityCrashReporter.init].
 */
data class LamityCrashReporterConfig(
    /** DSN events are sent to. Must not be blank. */
    val dsn: String,
    /** Deployment environment reported to Sentry, e.g. `production` or `development`. Must not be blank. */
    val environment: String,
    /**
     * Release identifier (typically `bundleId@versionName+versionCode`).
     * `null` leaves it unset; when provided it must not be blank.
     */
    val release: String?,
    /** Enables the Sentry SDK's own debug logging. */
    val debug: Boolean = false,
) {
    init {
        require(dsn.isNotBlank()) { "DSN must not be blank" }
        require(environment.isNotBlank()) { "Environment must not be blank" }
        require(release == null || release.isNotBlank()) {
            "Release must not be blank when provided"
        }
    }
}
