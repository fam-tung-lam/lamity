package com.phamtunglam.lamity.logger.logWriters

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.platformLogWriter
import com.phamtunglam.lamity.logger.mappers.toKermit
import com.phamtunglam.lamity.logger.models.LamityLogSeverity

/**
 * Default [LamityLogWriter] that mirrors records to the platform log (Logcat on
 * Android, NSLog/print on iOS, stdout on JVM).
 */
internal object PlatformLamityLogWriter : LamityLogWriter {
    private val delegate: LogWriter = platformLogWriter()

    override fun isLoggable(tag: String, severity: LamityLogSeverity): Boolean =
        delegate.isLoggable(tag, severity.toKermit())

    override fun log(
        severity: LamityLogSeverity,
        message: String,
        tag: String,
        throwable: Throwable?,
    ) = delegate.log(severity.toKermit(), message, tag, throwable)
}
