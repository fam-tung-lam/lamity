package com.phamtunglam.lamity.unitTests.feature.models.data

import co.touchlab.kermit.Logger
import co.touchlab.kermit.platformLogWriter
import com.phamtunglam.lamity.core.platform.AppDirs
import com.phamtunglam.lamity.core.platform.FileIo
import com.phamtunglam.lamity.downloader.Downloader
import com.phamtunglam.lamity.downloader.models.DownloadException
import com.phamtunglam.lamity.downloader.models.DownloadProgress
import com.phamtunglam.lamity.downloader.models.DownloadRequest
import com.phamtunglam.lamity.downloader.models.DownloadState
import com.phamtunglam.lamity.feature.models.data.ModelDownloadManager
import com.phamtunglam.lamity.feature.models.domain.ModelStatus
import com.phamtunglam.lamity.feature.models.data.ModelsRepository
import com.phamtunglam.lamity.feature.settings.domain.AppSettings
import com.phamtunglam.lamity.feature.settings.data.SettingsRepository
import com.phamtunglam.lamity.fixtures.advanceUntilIdle
import com.phamtunglam.lamity.fixtures.detachedTestScope
import com.phamtunglam.lamity.fixtures.fakeLlmModel
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.resetAnswers
import dev.mokkery.resetCalls
import dev.mokkery.verifySuspend
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow

class ModelDownloadManagerTest : BehaviorSpec({

    val fileIo = mock<FileIo>()
    val settings = mock<SettingsRepository>()
    val downloader = mock<Downloader>()
    val models = mock<ModelsRepository>()

    var lastRequest: DownloadRequest? = null

    beforeSpec {
        // Logcat is unavailable on JVM host tests; route Kermit logs nowhere.
        Logger.setLogWriters(listOf())
    }

    afterSpec {
        Logger.setLogWriters(platformLogWriter())
    }

    beforeEach {
        every { fileIo.mkdirs(any()) } returns Unit
        every { fileIo.delete(any()) } returns Unit
        every { fileIo.exists(any()) } returns false
        every { settings.value } returns AppSettings()
        every { models.models } returns MutableStateFlow(listOf(fakeLlmModel()))
        everySuspend { downloader.start(any()) } calls { (request: DownloadRequest) ->
            lastRequest = request
        }
    }

    afterEach {
        lastRequest = null
        resetAnswers(fileIo, settings, downloader, models)
        resetCalls(fileIo, settings, downloader, models)
    }

    suspend fun createManager(progress: MutableStateFlow<DownloadProgress?>) : ModelDownloadManager {
        every { downloader.observe(any()) } returns progress
        return ModelDownloadManager(
            dirs = AppDirs(dataDir = "/data", modelsDir = "/models", cacheDir = "/cache"),
            fileIo = fileIo,
            settings = settings,
            downloader = downloader,
            models = models,
            // Detached: the manager's eager stateIn would otherwise keep the test job alive.
            scope = detachedTestScope(),
        )
    }

    Given("a model download") {
        When("it starts for a HuggingFace model with a token configured") {
            Then("the request carries the token restricted to huggingface hosts") {
                every { settings.value } returns AppSettings(hfToken = "secret")
                val manager = createManager(MutableStateFlow(null))

                manager.start(fakeLlmModel())
                advanceUntilIdle()

                lastRequest!!.bearerToken shouldBe "secret"
                lastRequest!!.trustedAuthHosts shouldBe setOf("huggingface.co")
                lastRequest!!.destinationPath shouldBe "/models/m1.litertlm"
            }
        }

        When("it starts for a non-HuggingFace url") {
            Then("the request carries no token") {
                every { settings.value } returns AppSettings(hfToken = "secret")
                val manager = createManager(MutableStateFlow(null))

                manager.start(fakeLlmModel(url = "https://example.com/m1.litertlm"))
                advanceUntilIdle()

                lastRequest!!.bearerToken.shouldBeNull()
            }
        }

        When("wifi-only downloads are enabled") {
            Then("the request requires an unmetered network") {
                every { settings.value } returns AppSettings(wifiOnlyDownloads = true)
                val manager = createManager(MutableStateFlow(null))

                manager.start(fakeLlmModel())
                advanceUntilIdle()

                lastRequest!!.requireUnmetered shouldBe true
            }
        }
    }

    Given("the status map") {
        When("the downloader reports running progress") {
            Then("the model shows as downloading with rate and eta") {
                val progress = MutableStateFlow<DownloadProgress?>(null)
                val manager = createManager(progress)
                advanceUntilIdle()

                progress.value = DownloadProgress(
                    id = "model-1",
                    state = DownloadState.RUNNING,
                    downloadedBytes = 10,
                    totalBytes = 100,
                    bytesPerSecond = 5,
                    etaMillis = 18_000,
                )
                advanceUntilIdle()

                manager.statuses.value["model-1"] shouldBe ModelStatus.Downloading(
                    downloadedBytes = 10,
                    totalBytes = 100,
                    bytesPerSecond = 5,
                    etaMillis = 18_000,
                )
            }
        }

        When("the downloader reports success and the file landed") {
            Then("the model shows as downloaded") {
                every { fileIo.exists("/models/m1.litertlm") } returns true
                val progress = MutableStateFlow<DownloadProgress?>(null)
                val manager = createManager(progress)
                advanceUntilIdle()

                progress.value = DownloadProgress(id = "model-1", state = DownloadState.SUCCEEDED)
                advanceUntilIdle()

                manager.statuses.value["model-1"] shouldBe ModelStatus.Downloaded
            }
        }

        When("the file is deleted") {
            Then("the model returns to not downloaded") {
                every { fileIo.exists("/models/m1.litertlm") } returns true
                val manager = createManager(MutableStateFlow(null))
                advanceUntilIdle()
                manager.statuses.value["model-1"] shouldBe ModelStatus.Downloaded

                every { fileIo.exists("/models/m1.litertlm") } returns false
                manager.deleteFile(fakeLlmModel())
                advanceUntilIdle()

                manager.statuses.value["model-1"] shouldBe ModelStatus.NotDownloaded
            }
        }
    }

    Given("a paused download") {
        When("resuming finds no stored request") {
            Then("it restarts the download from scratch") {
                everySuspend { downloader.resume(any()) } throws DownloadException("nothing stored")
                val manager = createManager(MutableStateFlow(null))

                manager.resume(fakeLlmModel())
                advanceUntilIdle()

                verifySuspend { downloader.start(any()) }
            }
        }
    }
})
