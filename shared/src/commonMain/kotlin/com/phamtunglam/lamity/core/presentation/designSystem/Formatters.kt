package com.phamtunglam.lamity.core.presentation.designSystem

import kotlin.math.roundToInt

private const val BYTES_PER_GB = 1_073_741_824.0
private const val BYTES_PER_MB = 1_048_576.0
private const val BYTES_PER_KB = 1024.0
private const val ONE_DECIMAL_PLACE = 10.0

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "?"
    val gb = bytes / BYTES_PER_GB
    if (gb >= 1.0) return "${(gb * ONE_DECIMAL_PLACE).roundToInt() / ONE_DECIMAL_PLACE} GB"
    val mb = bytes / BYTES_PER_MB
    if (mb >= 1.0) return "${mb.roundToInt()} MB"
    return "${(bytes / BYTES_PER_KB).roundToInt()} KB"
}

fun formatPercent(received: Long, total: Long): String {
    if (total <= 0) return formatBytes(received)
    val pct = (received * 100.0 / total).roundToInt().coerceIn(0, 100)
    return "$pct% • ${formatBytes(received)} / ${formatBytes(total)}"
}

fun formatSpeed(bytesPerSecond: Long): String? = if (bytesPerSecond <= 0) null else "${formatBytes(bytesPerSecond)}/s"

fun formatEta(etaMillis: Long): String? {
    if (etaMillis <= 0) return null
    val totalSeconds = etaMillis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}
