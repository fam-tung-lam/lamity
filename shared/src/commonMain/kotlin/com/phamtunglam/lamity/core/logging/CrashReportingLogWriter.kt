package com.phamtunglam.lamity.core.logging

import com.phamtunglam.lamity.crashreporter.CrashReporter
import com.phamtunglam.lamity.logger.logWriters.LamityLogWriter
import com.phamtunglam.lamity.logger.models.LamityLogSeverity

/**
 * Bridges app logs into the crash reporter: every record becomes a breadcrumb
 * (so the tail of the log is attached to each crash), and error records
 * carrying a throwable are captured as events.
 *
 * Lives in `:shared` — the composition root and the only module that knows
 * about both logging ([LamityLogWriter]) and crash reporting ([CrashReporter]).
 * Keeping it here leaves `:lamityCrashReporter` free of any logging dependency
 * and `:lamityLogger` free of any crash-reporting dependency. Register it via
 * [com.phamtunglam.lamity.logger.LamityLogger.addWriter].
 */
class CrashReportingLogWriter(
    private val reporter: CrashReporter,
    private val minSeverity: LamityLogSeverity = LamityLogSeverity.Info,
) : LamityLogWriter {

    override fun isLoggable(tag: String, severity: LamityLogSeverity): Boolean =
        reporter.isEnabled && severity >= minSeverity

    override fun log(
        severity: LamityLogSeverity,
        message: String,
        tag: String,
        throwable: Throwable?,
    ) {
        reporter.addBreadcrumb(category = tag, message = message)
        if (severity >= LamityLogSeverity.Error && throwable != null) {
            reporter.captureException(throwable, mapOf("log.tag" to tag))
        }
    }
}
