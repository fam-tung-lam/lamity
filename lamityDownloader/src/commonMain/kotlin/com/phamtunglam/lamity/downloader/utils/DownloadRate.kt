package com.phamtunglam.lamity.downloader.utils

internal data class DownloadRateSnapshot(val bytesPerSecond: Long = 0, val etaMillis: Long = 0)

/**
 * Rolling transfer-rate estimator over the last [maxSamples] progress ticks;
 * smooths the bursty byte counts a network read loop produces.
 */
internal class DownloadRate(private val maxSamples: Int = SAMPLE_COUNT) {
    private val byteSamples = ArrayDeque<Long>()
    private val latencySamples = ArrayDeque<Long>()
    private var lastSampleMillis = 0L

    fun record(
        bytesRead: Long,
        downloadedBytes: Long,
        totalBytes: Long,
        nowMillis: Long,
    ): DownloadRateSnapshot {
        if (lastSampleMillis == 0L) {
            lastSampleMillis = nowMillis
            return DownloadRateSnapshot()
        }
        val latency = nowMillis - lastSampleMillis
        lastSampleMillis = nowMillis
        if (latency <= 0) return DownloadRateSnapshot()

        byteSamples.addLast(bytesRead)
        latencySamples.addLast(latency)
        while (byteSamples.size > maxSamples) {
            byteSamples.removeFirst()
            latencySamples.removeFirst()
        }

        val elapsedMillis = latencySamples.sum()
        if (elapsedMillis <= 0) return DownloadRateSnapshot()
        val bytesPerSecond = byteSamples.sum() * 1000 / elapsedMillis
        val etaMillis =
            if (bytesPerSecond > 0 && totalBytes > 0) {
                (totalBytes - downloadedBytes).coerceAtLeast(0) * 1000 / bytesPerSecond
            } else {
                0
            }
        return DownloadRateSnapshot(bytesPerSecond = bytesPerSecond, etaMillis = etaMillis)
    }

    private companion object {
        const val SAMPLE_COUNT = 5
    }
}
