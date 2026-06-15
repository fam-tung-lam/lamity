package com.phamtunglam.lamity.core.data.logging

import com.phamtunglam.lamity.crashreporter.LamityCrashReporter
import com.phamtunglam.lamity.logger.logWriters.LamityLogWriter
import com.phamtunglam.lamity.logger.models.LamityLogSeverity

/**
 * Bridges app logs into the crash reporter.
 */
class CrashReportingLogWriter(
    private val reporter: LamityCrashReporter,
    private val minSeverity: LamityLogSeverity = LamityLogSeverity.Error,
) : LamityLogWriter {
    override fun isLoggable(tag: String, severity: LamityLogSeverity): Boolean = severity >= minSeverity

    override fun log(
        severity: LamityLogSeverity,
        message: String,
        tag: String,
        throwable: Throwable?,
    ) {
        if (severity >= LamityLogSeverity.Error && throwable != null) {
            reporter.captureException(throwable)
        }
    }
}
