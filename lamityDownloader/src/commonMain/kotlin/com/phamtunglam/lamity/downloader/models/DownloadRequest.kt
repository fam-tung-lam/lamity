package com.phamtunglam.lamity.downloader.models

import kotlinx.serialization.Serializable

/**
 * One HTTP(S) file download to a local path.
 *
 * @property id stable key used to pause/resume/cancel/observe the download.
 * @property displayName human-readable name shown in platform notifications.
 * @property destinationPath final absolute path; the transfer itself writes to
 *   a temporary sibling and moves it here after (optional) verification.
 * @property bearerToken sent as `Authorization: Bearer …`, but only to
 *   [trustedAuthHosts]; redirects to other hosts have the header stripped
 *   (S3-style signed URLs reject requests that still carry one).
 * @property trustedAuthHosts host suffixes allowed to see [bearerToken];
 *   when empty, only the host of [url] itself is trusted.
 * @property expectedSizeBytes used for progress totals when the server does
 *   not report a length; 0 when unknown. Not used for hard verification.
 * @property sha256 expected hex digest (any case); when set, the file is
 *   verified before being moved into place.
 * @property requireUnmetered when true the transfer only runs on unmetered
 *   networks (Wi-Fi); on Android it also waits for one.
 */
@Serializable
data class DownloadRequest(
    val id: String,
    val url: String,
    val destinationPath: String,
    val displayName: String = id,
    val headers: Map<String, String> = emptyMap(),
    val bearerToken: String? = null,
    val trustedAuthHosts: Set<String> = emptySet(),
    val expectedSizeBytes: Long = 0,
    val sha256: String? = null,
    val requireUnmetered: Boolean = false,
)

/** True when [host] may receive the Authorization header for this request. */
internal fun DownloadRequest.isAuthTrustedHost(host: String?): Boolean {
    if (host.isNullOrBlank()) return false
    val trusted = trustedAuthHosts.ifEmpty { setOfNotNull(hostOf(url)) }
    return trusted.any { host == it || host.endsWith(".$it") }
}

internal fun hostOf(url: String): String? =
    url.substringAfter("://", "").substringBefore('/').substringBefore('?')
        .substringAfterLast('@').substringBefore(':').ifBlank { null }
