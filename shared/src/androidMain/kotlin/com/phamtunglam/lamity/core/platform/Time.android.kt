package com.phamtunglam.lamity.core.platform

import java.text.DateFormat
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

actual fun epochMillis(): Long = System.currentTimeMillis()

actual fun formatDateTime(epochMillis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(epochMillis))

actual fun currentTimeInfo(timeZoneId: String?): TimeToolInfo? {
    val zone = if (timeZoneId.isNullOrBlank()) {
        ZoneId.systemDefault()
    } else {
        runCatching { ZoneId.of(timeZoneId) }.getOrNull() ?: return null
    }
    val now = ZonedDateTime.now(zone)
    return TimeToolInfo(
        iso = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
        date = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
        time = now.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
        dayOfWeek = now.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
        timeZone = zone.id,
        utcOffset = now.offset.id,
    )
}
