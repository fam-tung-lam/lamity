package com.phamtunglam.lamity.crashreporter.fixtures

import com.phamtunglam.lamity.crashreporter.extensions.applyTo
import com.phamtunglam.lamity.crashreporter.models.LamityCrashReporterConfig
import io.sentry.kotlin.multiplatform.SentryOptions

/** A valid config; override a single field to exercise one rule at a time. */
internal fun fakeCrashReporterConfig(
    dsn: String = "https://key@o0.ingest.sentry.io/1",
    environment: String = "production",
    release: String? = "com.app@1.0.0+1",
    debug: Boolean = false,
) = LamityCrashReporterConfig(
    dsn = dsn,
    environment = environment,
    release = release,
    debug = debug,
)

/**
 * Sentry options whose fields are deliberately set to values that differ from
 * both the SDK defaults and from what [LamityCrashReporterConfig.applyTo] writes.
 */
internal fun fakeSentryOptions() =
    SentryOptions().apply {
        dsn = "stale-dsn"
        environment = "stale-env"
        release = "stale-release"
        debug = true
        attachStackTrace = false
        attachThreads = false
        enableAutoSessionTracking = false
        sessionTrackingIntervalMillis = 1L
        isAnrEnabled = false
        anrTimeoutIntervalMillis = 1L
        enableAppHangTracking = false
        appHangTimeoutIntervalMillis = 1L
        enableWatchdogTerminationTracking = false
    }
