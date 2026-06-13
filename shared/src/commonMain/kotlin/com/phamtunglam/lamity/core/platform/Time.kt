package com.phamtunglam.lamity.core.platform

/** Current wall-clock time in milliseconds since the Unix epoch. */
expect fun epochMillis(): Long

/** Localized, short "date, time" string for the given epoch millis (used in history lists). */
expect fun formatDateTime(epochMillis: Long): String

/** Payload returned by the get_current_time tool. */
data class TimeToolInfo(
    val iso: String,
    val date: String,
    val time: String,
    val dayOfWeek: String,
    val timeZone: String,
    val utcOffset: String,
)

/**
 * Snapshot of "now" in [timeZoneId] (IANA id) or the system zone when null.
 * Returns null when the zone id is unknown.
 */
expect fun currentTimeInfo(timeZoneId: String?): TimeToolInfo?
