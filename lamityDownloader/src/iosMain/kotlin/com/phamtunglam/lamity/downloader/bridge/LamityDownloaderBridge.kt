package com.phamtunglam.lamity.downloader.bridge

/**
 * Contract implemented in Swift over a background `URLSession`
 * (iosApp/Downloader/LamityDownloaderBridgeImpl.swift). Swift owns transfer,
 * resume data, SHA-256 verification and the final file move; Kotlin only maps
 * states. Parameters are flat platform types so the framework header stays
 * Swift-friendly.
 */
interface LamityDownloaderBridge {
    @Suppress("LongParameterList")
    fun start(
        id: String,
        url: String,
        destinationPath: String,
        headerKeys: List<String>,
        headerValues: List<String>,
        bearerToken: String?,
        trustedAuthHosts: List<String>,
        expectedSizeBytes: Long,
        sha256: String?,
        requireUnmetered: Boolean,
    )

    fun pause(id: String)

    fun resume(id: String)

    fun cancel(id: String)

    /**
     * Registers the single progress observer for [id] (replacing any previous
     * one) and immediately reports the last known state, if any. [onProgress]
     * receives a [com.phamtunglam.lamity.downloader.models.DownloadState] name.
     */
    fun observe(
        id: String,
        onProgress: (
            state: String,
            downloadedBytes: Long,
            totalBytes: Long,
            bytesPerSecond: Long,
            etaMillis: Long,
        ) -> Unit,
        onError: (message: String) -> Unit,
    )
}

/** Holds the Swift bridge; assigned by `DownloaderBootstrap.install()` at app startup. */
object LamityDownloaderIos {
    var bridge: LamityDownloaderBridge? = null

    internal fun requireBridge(): LamityDownloaderBridge =
        checkNotNull(bridge) {
            "LamityDownloaderBridge is not set. Call DownloaderBootstrap.install() at app startup."
        }
}
