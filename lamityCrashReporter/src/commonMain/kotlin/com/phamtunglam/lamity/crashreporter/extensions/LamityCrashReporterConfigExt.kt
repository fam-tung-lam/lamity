package com.phamtunglam.lamity.crashreporter.extensions

import com.phamtunglam.lamity.crashreporter.models.LamityCrashReporterConfig
import io.sentry.kotlin.multiplatform.SentryOptions

/**
 * Translates a [LamityCrashReporterConfig] into Sentry's [SentryOptions],
 * applying it to the [options] instance handed out by `Sentry.init`.
 */
internal fun LamityCrashReporterConfig.applyTo(options: SentryOptions) {
    options.dsn = dsn
    options.environment = environment
    release?.let { options.release = it }
    options.debug = debug

    // Default values.
    options.attachStackTrace = true
    options.attachThreads = true
    options.enableAutoSessionTracking = true
    options.sessionTrackingIntervalMillis = 30_000L
    options.isAnrEnabled = true
    options.anrTimeoutIntervalMillis = 5_000L
    options.enableAppHangTracking = true
    options.appHangTimeoutIntervalMillis = 2_000L
    options.enableWatchdogTerminationTracking = true
}
