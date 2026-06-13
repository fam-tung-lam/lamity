package com.phamtunglam.lamity.core.platform

import kotlin.math.abs
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.localTimeZone
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.timeZoneWithName

actual fun epochMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

actual fun formatDateTime(epochMillis: Long): String {
    val formatter = NSDateFormatter().apply {
        dateStyle = NSDateFormatterShortStyle
        timeStyle = NSDateFormatterShortStyle
    }
    return formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(epochMillis / 1000.0))
}

actual fun currentTimeInfo(timeZoneId: String?): TimeToolInfo? {
    val zone = if (timeZoneId.isNullOrBlank()) {
        NSTimeZone.localTimeZone
    } else {
        NSTimeZone.timeZoneWithName(timeZoneId) ?: return null
    }
    val now = NSDate()

    fun fmt(pattern: String): String = NSDateFormatter().apply {
        dateFormat = pattern
        timeZone = zone
        locale = NSLocale.currentLocale
    }.stringFromDate(now)

    val offsetSeconds = zone.secondsFromGMTForDate(now).toInt()
    val sign = if (offsetSeconds >= 0) "+" else "-"
    val hours = (abs(offsetSeconds) / 3600).toString().padStart(2, '0')
    val minutes = ((abs(offsetSeconds) % 3600) / 60).toString().padStart(2, '0')

    return TimeToolInfo(
        iso = fmt("yyyy-MM-dd'T'HH:mm:ssZZZZZ"),
        date = fmt("yyyy-MM-dd"),
        time = fmt("HH:mm:ss"),
        dayOfWeek = fmt("EEEE"),
        timeZone = zone.name,
        utcOffset = "$sign$hours:$minutes",
    )
}
