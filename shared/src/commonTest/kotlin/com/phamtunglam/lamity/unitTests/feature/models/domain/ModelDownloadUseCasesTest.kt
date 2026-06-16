package com.phamtunglam.lamity.unitTests.feature.models.domain

import com.phamtunglam.lamity.core.domain.platform.AppDirs
import com.phamtunglam.lamity.downloader.Downloader
import com.phamtunglam.lamity.downloader.models.DownloadException
import com.phamtunglam.lamity.downloader.models.DownloadRequest
import com.phamtunglam.lamity.feature.models.data.ModelFiles
import com.phamtunglam.lamity.feature.models.domain.ResumeModelDownloadUseCase
import com.phamtunglam.lamity.feature.models.domain.StartModelDownloadUseCase
import com.phamtunglam.lamity.feature.settings.data.SettingsRepository
import com.phamtunglam.lamity.feature.settings.domain.AppSettings
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
import okio.fakefilesystem.FakeFileSystem

class ModelDownloadUseCasesTest :
    BehaviorSpec({

        val fileSystem = FakeFileSystem()
        val settings = mock<SettingsRepository>()
        val downloader = mock<Downloader>()

        var lastRequest: DownloadRequest? = null

        beforeEach {
            every { settings.value } returns AppSettings()
            everySuspend { downloader.start(any()) } calls { (request: DownloadRequest) -> lastRequest = request }
        }

        afterEach {
            lastRequest = null
            resetAnswers(settings, downloader)
            resetCalls(settings, downloader)
        }

        fun modelFiles() =
            ModelFiles(AppDirs(dataDir = "/data", modelsDir = "/models", cacheDir = "/cache"), fileSystem)

        fun startUseCase(hfToken: String = "") = StartModelDownloadUseCase(downloader, settings, modelFiles(), hfToken)

        Given("starting a download") {
            When("for a HuggingFace model with a token configured") {
                Then("the request carries the token restricted to huggingface hosts") {
                    startUseCase(hfToken = "secret").invoke(fakeLlmModel())

                    lastRequest!!.bearerToken shouldBe "secret"
                    lastRequest!!.trustedAuthHosts shouldBe setOf("huggingface.co")
                    lastRequest!!.destinationPath shouldBe "/models/m1.litertlm"
                }
            }

            When("for a non-HuggingFace url") {
                Then("the request carries no token") {
                    startUseCase(hfToken = "secret").invoke(fakeLlmModel(url = "https://example.com/m1.litertlm"))

                    lastRequest!!.bearerToken.shouldBeNull()
                }
            }

            When("wifi-only downloads are enabled") {
                Then("the request requires an unmetered network") {
                    every { settings.value } returns AppSettings(wifiOnlyDownloads = true)

                    startUseCase().invoke(fakeLlmModel())

                    lastRequest!!.requireUnmetered shouldBe true
                }
            }
        }

        Given("a paused download") {
            When("resuming finds no stored request") {
                Then("it restarts the download from scratch") {
                    everySuspend { downloader.resume(any()) } throws DownloadException("nothing stored")
                    val resume = ResumeModelDownloadUseCase(downloader, startUseCase())

                    resume(fakeLlmModel())

                    verifySuspend { downloader.start(any()) }
                }
            }
        }
    })
