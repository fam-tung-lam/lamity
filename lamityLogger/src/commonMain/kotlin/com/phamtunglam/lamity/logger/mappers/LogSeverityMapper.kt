package com.phamtunglam.lamity.logger.mappers

import co.touchlab.kermit.Severity
import com.phamtunglam.lamity.logger.models.LamityLogSeverity

/** Maps a Kermit [Severity] to its [LamityLogSeverity] equivalent. */
internal fun Severity.toLamity(): LamityLogSeverity =
    when (this) {
        Severity.Verbose -> LamityLogSeverity.Verbose
        Severity.Debug -> LamityLogSeverity.Debug
        Severity.Info -> LamityLogSeverity.Info
        Severity.Warn -> LamityLogSeverity.Warn
        Severity.Error -> LamityLogSeverity.Error
        Severity.Assert -> LamityLogSeverity.Assert
    }

/** Maps a [LamityLogSeverity] back to its Kermit [Severity] equivalent. */
internal fun LamityLogSeverity.toKermit(): Severity =
    when (this) {
        LamityLogSeverity.Verbose -> Severity.Verbose
        LamityLogSeverity.Debug -> Severity.Debug
        LamityLogSeverity.Info -> Severity.Info
        LamityLogSeverity.Warn -> Severity.Warn
        LamityLogSeverity.Error -> Severity.Error
        LamityLogSeverity.Assert -> Severity.Assert
    }
