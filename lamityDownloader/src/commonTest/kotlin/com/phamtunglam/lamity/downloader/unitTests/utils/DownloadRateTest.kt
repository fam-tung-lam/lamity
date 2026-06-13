package com.phamtunglam.lamity.downloader.unitTests.utils

import com.phamtunglam.lamity.downloader.utils.DownloadRate
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class DownloadRateTest : BehaviorSpec({

    Given("a rolling download-rate estimator") {
        When("the first tick is recorded") {
            Then("it reports no rate yet") {
                val rate = DownloadRate()

                val snapshot = rate.record(
                    bytesRead = 100,
                    downloadedBytes = 100,
                    totalBytes = 1_000,
                    nowMillis = 1_000,
                )

                snapshot.bytesPerSecond shouldBe 0
                snapshot.etaMillis shouldBe 0
            }
        }

        When("a second tick arrives after a known latency") {
            Then("it derives bytes per second and the remaining time") {
                val rate = DownloadRate()
                rate.record(bytesRead = 100, downloadedBytes = 100, totalBytes = 1_000, nowMillis = 1_000)

                val snapshot = rate.record(
                    bytesRead = 200,
                    downloadedBytes = 300,
                    totalBytes = 1_000,
                    nowMillis = 1_500,
                )

                snapshot.bytesPerSecond shouldBe 400 // 200 bytes over 500 ms
                snapshot.etaMillis shouldBe 1_750 // 700 bytes left at 400 B/s
            }
        }

        When("more ticks arrive than the sample window holds") {
            Then("it only weighs the trailing samples") {
                val rate = DownloadRate(maxSamples = 2)
                rate.record(bytesRead = 1, downloadedBytes = 1, totalBytes = 0, nowMillis = 0)
                rate.record(bytesRead = 1_000_000, downloadedBytes = 1_000_001, totalBytes = 0, nowMillis = 1_000)

                val snapshot = rate.record(
                    bytesRead = 100,
                    downloadedBytes = 1_000_101,
                    totalBytes = 0,
                    nowMillis = 2_000,
                )
                val slowTail = rate.record(
                    bytesRead = 100,
                    downloadedBytes = 1_000_201,
                    totalBytes = 0,
                    nowMillis = 3_000,
                )

                snapshot.bytesPerSecond shouldBe 500_050 // window still holds the burst
                slowTail.bytesPerSecond shouldBe 100 // burst dropped from the window
            }
        }

        When("the total size is unknown") {
            Then("it reports a rate but no eta") {
                val rate = DownloadRate()
                rate.record(bytesRead = 100, downloadedBytes = 100, totalBytes = 0, nowMillis = 1_000)

                val snapshot = rate.record(
                    bytesRead = 100,
                    downloadedBytes = 200,
                    totalBytes = 0,
                    nowMillis = 2_000,
                )

                snapshot.bytesPerSecond shouldBe 100
                snapshot.etaMillis shouldBe 0
            }
        }
    }
})
