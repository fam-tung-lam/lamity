package com.phamtunglam.lamity.crashreporter.extensions

import com.phamtunglam.lamity.crashreporter.models.LamityCrashReporterConfig
import io.sentry.kotlin.multiplatform.SentryOptions

private const val SESSION_TRACKING_INTERVAL_MILLIS = 30_000L
private const val ANR_TIMEOUT_INTERVAL_MILLIS = 5_000L
private const val APP_HANG_TIMEOUT_INTERVAL_MILLIS = 2_000L

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
    options.sessionTrackingIntervalMillis = SESSION_TRACKING_INTERVAL_MILLIS
    options.isAnrEnabled = true
    options.anrTimeoutIntervalMillis = ANR_TIMEOUT_INTERVAL_MILLIS
    options.enableAppHangTracking = true
    options.appHangTimeoutIntervalMillis = APP_HANG_TIMEOUT_INTERVAL_MILLIS
    options.enableWatchdogTerminationTracking = true
}
