package com.phamtunglam.lamity.crashreporter

import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.protocol.Breadcrumb

/**
 * Crash-reporting facade. The app codes against this interface; the concrete
 * backend is the Sentry Kotlin Multiplatform SDK (sentry-android on Android,
 * Sentry Cocoa on iOS), created with [sentryCrashReporter].
 */
interface CrashReporter {
    val isEnabled: Boolean
    fun captureException(throwable: Throwable, tags: Map<String, String> = emptyMap())
    fun captureMessage(message: String, tags: Map<String, String> = emptyMap())
    fun addBreadcrumb(category: String, message: String)
    fun setTag(key: String, value: String)
}

data class CrashReporterConfig(
    /** Sentry DSN. Blank disables reporting entirely (every call becomes a no-op). */
    val dsn: String,
    val environment: String = "production",
    val release: String = "",
    val debug: Boolean = false,
)

/**
 * Initializes the Sentry KMP SDK and returns a [CrashReporter] backed by it.
 */
fun sentryCrashReporter(config: CrashReporterConfig): CrashReporter {
    if (config.dsn.isBlank()) throw IllegalArgumentException("")
    Sentry.init { options ->
        options.dsn = config.dsn
        options.environment = config.environment
        if (config.release.isNotBlank()) options.release = config.release
        options.debug = config.debug
        options.attachStackTrace = true
        options.attachThreads = true
        options.enableAutoSessionTracking = true
        options.sessionTrackingIntervalMillis = 30_000L
        // Hang/ANR detection: ANR is Android-only, app-hang iOS-only; the KMP
        // SDK routes each flag to the matching platform.
        options.isAnrEnabled = true
        options.anrTimeoutIntervalMillis = 5_000L
        options.enableAppHangTracking = true
        options.appHangTimeoutIntervalMillis = 2_000L
        options.enableWatchdogTerminationTracking = true
    }
    return SentryCrashReporter
}

private object SentryCrashReporter : CrashReporter {
    override val isEnabled: Boolean = true

    override fun captureException(throwable: Throwable, tags: Map<String, String>) {
        Sentry.captureException(throwable) { scope ->
            tags.forEach { (key, value) -> scope.setTag(key, value) }
        }
    }

    override fun captureMessage(message: String, tags: Map<String, String>) {
        Sentry.captureMessage(message) { scope ->
            tags.forEach { (key, value) -> scope.setTag(key, value) }
        }
    }

    override fun addBreadcrumb(category: String, message: String) {
        Sentry.addBreadcrumb(Breadcrumb(category = category, message = message))
    }

    override fun setTag(key: String, value: String) {
        Sentry.configureScope { scope -> scope.setTag(key, value) }
    }
}
