package com.phamtunglam.lamity.downloader.states

import com.phamtunglam.lamity.downloader.models.DownloadState
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class DownloadStateFlagsTest :
    BehaviorSpec({

        Given("platform scheduler flags") {
            When("they are folded into a download state") {
                Then("terminal outcomes win over everything else") {
                    DownloadStateFlags(succeeded = true, hasPartial = true)
                        .toDownloadState() shouldBe DownloadState.SUCCEEDED
                    DownloadStateFlags(failed = true, hasPartial = true)
                        .toDownloadState() shouldBe DownloadState.FAILED
                }
                Then("an active transfer reports running") {
                    DownloadStateFlags(running = true).toDownloadState() shouldBe DownloadState.RUNNING
                }
                Then("a scheduled transfer reports queued") {
                    DownloadStateFlags(enqueued = true).toDownloadState() shouldBe DownloadState.QUEUED
                }
                Then("a cancelled transfer with partial bytes is resumable as paused") {
                    DownloadStateFlags(cancelled = true, hasPartial = true)
                        .toDownloadState() shouldBe DownloadState.PAUSED
                }
                Then("a cancelled transfer without partial bytes is disposed") {
                    DownloadStateFlags(cancelled = true)
                        .toDownloadState() shouldBe DownloadState.CANCELLED
                }
                Then("leftover partial bytes alone count as paused") {
                    DownloadStateFlags(hasPartial = true).toDownloadState() shouldBe DownloadState.PAUSED
                }
            }
        }
    })
