package com.phamtunglam.lamity.logger.models

/**
 * Log severity levels exposed by the Lamity logging facade.
 */
enum class LamityLogSeverity {
    /** Fine-grained tracing detail; the least severe level. */
    Verbose,

    /** Diagnostic information useful while debugging. */
    Debug,

    /** Informational messages about normal operation. */
    Info,

    /** Unexpected but recoverable conditions worth attention. */
    Warn,

    /** Failures that prevented an operation from completing. */
    Error,

    /** Conditions that should never occur; the most severe level. */
    Assert,
}
