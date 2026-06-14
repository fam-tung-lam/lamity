package com.phamtunglam.lamity.core.domain.platform

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.offsetIn
import kotlinx.datetime.toLocalDateTime

/** Current wall-clock time in milliseconds since the Unix epoch. */
fun epochMillis(): Long = Clock.System.now().toEpochMilliseconds()

/** Locale-invariant "yyyy-MM-dd HH:mm" rendering for the history list. */
private val historyDateTime = LocalDateTime.Format {
    date(LocalDate.Formats.ISO)
    char(' ')
    hour()
    char(':')
    minute()
}

/** Short, locale-invariant "yyyy-MM-dd HH:mm" string for the given epoch millis (used in history lists). */
fun formatDateTime(epochMillis: Long): String =
    Instant.fromEpochMilliseconds(epochMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .format(historyDateTime)

/** Payload returned by the get_current_time tool. */
data class TimeToolInfo(
    val iso: String,
    val date: String,
    val time: String,
    val dayOfWeek: String,
    val timeZone: String,
    val utcOffset: String,
)

private val isoTime = LocalTime.Format {
    hour()
    char(':')
    minute()
    char(':')
    second()
}

private val isoOffset = UtcOffset.Format {
    offsetHours()
    char(':')
    offsetMinutesOfHour()
}

/**
 * Snapshot of "now" in [timeZoneId] (IANA id) or the system zone when null.
 * Returns null when the zone id is unknown.
 */
fun currentTimeInfo(timeZoneId: String?): TimeToolInfo? {
    val zone = if (timeZoneId.isNullOrBlank()) {
        TimeZone.currentSystemDefault()
    } else {
        runCatching { TimeZone.of(timeZoneId) }.getOrNull() ?: return null
    }
    val now = Clock.System.now()
    val offset = now.offsetIn(zone)
    val local = now.toLocalDateTime(zone)
    val date = local.date.format(LocalDate.Formats.ISO)
    val time = local.time.format(isoTime)
    val utcOffset = offset.format(isoOffset)
    return TimeToolInfo(
        iso = "${date}T$time$utcOffset",
        date = date,
        time = time,
        dayOfWeek = local.dayOfWeek.name.lowercase().replaceFirstChar(Char::uppercase),
        timeZone = zone.id,
        utcOffset = utcOffset,
    )
}
