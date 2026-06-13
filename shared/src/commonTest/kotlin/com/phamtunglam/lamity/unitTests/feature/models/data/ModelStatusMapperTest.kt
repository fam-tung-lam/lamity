package com.phamtunglam.lamity.unitTests.feature.models.data

import com.phamtunglam.lamity.downloader.models.DownloadProgress
import com.phamtunglam.lamity.downloader.models.DownloadState
import com.phamtunglam.lamity.feature.models.data.ModelStatusMapper
import com.phamtunglam.lamity.feature.models.domain.ModelStatus
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class ModelStatusMapperTest : BehaviorSpec({

    fun progress(state: DownloadState, error: String? = null) = DownloadProgress(
        id = "m1",
        state = state,
        downloadedBytes = 10,
        totalBytes = 100,
        bytesPerSecond = 5,
        etaMillis = 18_000,
        error = error,
    )

    Given("downloader progress and on-disk presence") {
        When("nothing is known about the download") {
            Then("the file decides between downloaded and not downloaded") {
                ModelStatusMapper.map(null, fileExists = true) shouldBe ModelStatus.Downloaded
                ModelStatusMapper.map(null, fileExists = false) shouldBe ModelStatus.NotDownloaded
            }
        }

        When("the transfer is active") {
            Then("queued and running map with their progress numbers") {
                ModelStatusMapper.map(progress(DownloadState.QUEUED), false) shouldBe ModelStatus.Queued
                ModelStatusMapper.map(progress(DownloadState.RUNNING), false) shouldBe
                    ModelStatus.Downloading(
                        downloadedBytes = 10,
                        totalBytes = 100,
                        bytesPerSecond = 5,
                        etaMillis = 18_000,
                    )
            }
            Then("paused and verifying map directly") {
                ModelStatusMapper.map(progress(DownloadState.PAUSED), false) shouldBe
                    ModelStatus.Paused(downloadedBytes = 10, totalBytes = 100)
                ModelStatusMapper.map(progress(DownloadState.VERIFYING), false) shouldBe
                    ModelStatus.Verifying
            }
        }

        When("the transfer ended") {
            Then("success still requires the file to exist") {
                ModelStatusMapper.map(progress(DownloadState.SUCCEEDED), true) shouldBe
                    ModelStatus.Downloaded
                ModelStatusMapper.map(progress(DownloadState.SUCCEEDED), false) shouldBe
                    ModelStatus.NotDownloaded
            }
            Then("a failure carries its message and cancellation falls back to the file") {
                ModelStatusMapper.map(progress(DownloadState.FAILED, error = "HTTP 403"), false) shouldBe
                    ModelStatus.Failed("HTTP 403")
                ModelStatusMapper.map(progress(DownloadState.CANCELLED), false) shouldBe
                    ModelStatus.NotDownloaded
            }
        }
    }
})
