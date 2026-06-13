package com.phamtunglam.lamity.logger.mappers

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import com.phamtunglam.lamity.logger.logWriters.LamityLogWriter

/**
 * Adapts a [LamityLogWriter] to a Kermit [LogWriter], translating severities at
 * the boundary so the wrapped writer never sees a Kermit type.
 */
internal fun LamityLogWriter.asKermitWriter(): LogWriter {
    val writer = this
    return object : LogWriter() {
        override fun isLoggable(tag: String, severity: Severity): Boolean =
            writer.isLoggable(tag, severity.toLamity())

        override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) =
            writer.log(severity.toLamity(), message, tag, throwable)
    }
}
