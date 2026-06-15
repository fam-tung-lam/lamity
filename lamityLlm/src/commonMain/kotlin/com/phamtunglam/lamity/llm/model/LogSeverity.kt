package com.phamtunglam.lamity.llm.model

/** Minimum log level for the native LiteRT-LM library. [level] is the value the C API expects. */
@Suppress("MagicNumber")
enum class LogSeverity(val level: Int) {
    Verbose(0),
    Debug(1),
    Info(2),
    Warning(3),
    Error(4),
    Fatal(5),
    Silent(1000),
}
