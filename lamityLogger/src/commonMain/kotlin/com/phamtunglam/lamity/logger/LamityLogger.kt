package com.phamtunglam.lamity.logger

import androidx.annotation.VisibleForTesting
import co.touchlab.kermit.Logger
import com.phamtunglam.lamity.logger.logWriters.LamityLogWriter
import com.phamtunglam.lamity.logger.mappers.asKermitWriter
import com.phamtunglam.lamity.logger.mappers.toKermit
import com.phamtunglam.lamity.logger.models.LamityLogSeverity

/**
 * App-wide logging facade wrapping the Kermit logging library.
 *
 * Logging is active only while at least one writer is registered via
 * [addWriter]; with no writers, records are dropped without evaluating their
 * message. Registering a writer enables logging; removing the last one disables it.
 *
 * Intentionally decoupled from Kermit (the logging backend) so other modules
 * can implement it without depending on the backend.
 */
object LamityLogger {
    /** Registered app writers, in registration order. */
    @VisibleForTesting
    internal val writers = mutableListOf<LamityLogWriter>()

    /** Registers [writer] to receive subsequent log records. No-op if already registered. */
    fun addWriter(writer: LamityLogWriter) {
        if (writer in writers) return
        writers += writer
        setKermitWriters()
    }

    /** Unregisters a previously [added][addWriter] writer. No-op if not registered. */
    fun removeWriter(writer: LamityLogWriter) {
        if (writers.remove(writer)) setKermitWriters()
    }

    /** Logs [message] at verbose severity under [tag], optionally attaching [throwable]. */
    fun v(tag: String, throwable: Throwable? = null, message: () -> String) = log { Logger.v(throwable, tag, message) }

    /** Logs [message] at debug severity under [tag], optionally attaching [throwable]. */
    fun d(tag: String, throwable: Throwable? = null, message: () -> String) = log { Logger.d(throwable, tag, message) }

    /** Logs [message] at info severity under [tag], optionally attaching [throwable]. */
    fun i(tag: String, throwable: Throwable? = null, message: () -> String) = log { Logger.i(throwable, tag, message) }

    /** Logs [message] at warn severity under [tag], optionally attaching [throwable]. */
    fun w(tag: String, throwable: Throwable? = null, message: () -> String) = log { Logger.w(throwable, tag, message) }

    /** Logs [message] at error severity under [tag], optionally attaching [throwable]. */
    fun e(tag: String, throwable: Throwable? = null, message: () -> String) = log { Logger.e(throwable, tag, message) }

    /** Invokes [kermitLog] only while at least one writer is registered. */
    private inline fun log(kermitLog: () -> Unit) {
        if (writers.isNotEmpty()) kermitLog()
    }

    /** Rebuilds Kermit's writer list from the registered [writers]. */
    private fun setKermitWriters() {
        Logger.setLogWriters(writers.map { it.asKermitWriter() })
    }

    /**
     * Replaces the entire registered writer set with [writers].
     *
     * Test hook mirroring the backend's writer reset, so tests can wipe this
     * singleton's global state between cases without touching Kermit directly.
     */
    @VisibleForTesting
    internal fun setLogWriters(writers: List<LamityLogWriter>) {
        this.writers.clear()
        this.writers.addAll(writers)
        setKermitWriters()
    }
}
