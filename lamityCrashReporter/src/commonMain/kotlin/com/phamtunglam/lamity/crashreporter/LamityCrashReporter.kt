package com.phamtunglam.lamity.crashreporter

import com.phamtunglam.lamity.crashreporter.extensions.applyTo
import com.phamtunglam.lamity.crashreporter.models.LamityCrashReporterConfig
import io.sentry.kotlin.multiplatform.Sentry

/**
 * App-wide crash reporter backed by Sentry.
 */
object LamityCrashReporter {
    /** Initializes crash reporting from [config]. Call once at app start. */
    fun init(config: LamityCrashReporterConfig) {
        Sentry.init { options -> config.applyTo(options) }
    }

    /** Reports [throwable] as an error event. */
    fun captureException(throwable: Throwable) {
        Sentry.captureException(throwable)
    }
}
