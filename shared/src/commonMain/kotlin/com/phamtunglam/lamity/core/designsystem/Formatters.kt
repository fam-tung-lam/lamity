package com.phamtunglam.lamity.core.designsystem

import kotlin.math.roundToInt

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "?"
    val gb = bytes / 1_073_741_824.0
    if (gb >= 1.0) return "${(gb * 10).roundToInt() / 10.0} GB"
    val mb = bytes / 1_048_576.0
    if (mb >= 1.0) return "${mb.roundToInt()} MB"
    return "${(bytes / 1024.0).roundToInt()} KB"
}

fun formatPercent(received: Long, total: Long): String {
    if (total <= 0) return formatBytes(received)
    val pct = (received * 100.0 / total).roundToInt().coerceIn(0, 100)
    return "$pct% • ${formatBytes(received)} / ${formatBytes(total)}"
}

fun formatSpeed(bytesPerSecond: Long): String? =
    if (bytesPerSecond <= 0) null else "${formatBytes(bytesPerSecond)}/s"

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
