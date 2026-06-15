package com.phamtunglam.lamity.downloader.unitTests.states

import com.phamtunglam.lamity.downloader.models.DownloadState
import com.phamtunglam.lamity.downloader.states.DownloadStateFlags
import com.phamtunglam.lamity.downloader.states.toDownloadState
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
                Then("an active transfer reports running, a scheduled one queued") {
                    DownloadStateFlags(running = true).toDownloadState() shouldBe DownloadState.RUNNING
                    DownloadStateFlags(enqueued = true).toDownloadState() shouldBe DownloadState.QUEUED
                }
                Then("a cancelled transfer with partial bytes is resumable, without them disposed") {
                    DownloadStateFlags(cancelled = true, hasPartial = true)
                        .toDownloadState() shouldBe DownloadState.PAUSED
                    DownloadStateFlags(cancelled = true)
                        .toDownloadState() shouldBe DownloadState.CANCELLED
                }
                Then("leftover partial bytes alone count as paused") {
                    DownloadStateFlags(hasPartial = true).toDownloadState() shouldBe DownloadState.PAUSED
                }
            }
        }
    })
