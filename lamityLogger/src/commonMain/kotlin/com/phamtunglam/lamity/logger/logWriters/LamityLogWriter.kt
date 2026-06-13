package com.phamtunglam.lamity.logger.logWriters

import com.phamtunglam.lamity.logger.models.LamityLogSeverity
import com.phamtunglam.lamity.logger.LamityLogger

/**
 * A sink for log records routed through the Lamity logging facade.
 *
 * Implementations act as log writers/processors — e.g. forwarding records to a
 * crash reporter, an analytics pipeline, or a file — and are
 * registered / unregistered via [LamityLogger.addWriter] / [LamityLogger.removeWriter].
 */
interface LamityLogWriter {
    /** Whether this writer wants records for [tag] at [severity]. Defaults to accepting all records. */
    fun isLoggable(tag: String, severity: LamityLogSeverity): Boolean = true

    /** Processes a single log record. */
    fun log(severity: LamityLogSeverity, message: String, tag: String, throwable: Throwable?)
}
