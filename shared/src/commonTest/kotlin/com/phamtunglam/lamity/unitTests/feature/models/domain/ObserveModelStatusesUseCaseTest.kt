package com.phamtunglam.lamity.unitTests.feature.models.domain

import com.phamtunglam.lamity.core.domain.platform.AppDirs
import com.phamtunglam.lamity.downloader.Downloader
import com.phamtunglam.lamity.downloader.models.DownloadProgress
import com.phamtunglam.lamity.downloader.models.DownloadState
import com.phamtunglam.lamity.feature.models.data.ModelFiles
import com.phamtunglam.lamity.feature.models.data.ModelsRepository
import com.phamtunglam.lamity.feature.models.domain.ModelStatus
import com.phamtunglam.lamity.feature.models.domain.ObserveModelStatusesUseCase
import com.phamtunglam.lamity.fixtures.advanceUntilIdle
import com.phamtunglam.lamity.fixtures.detachedTestScope
import com.phamtunglam.lamity.fixtures.fakeLlmModel
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.resetAnswers
import dev.mokkery.resetCalls
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

class ObserveModelStatusesUseCaseTest :
    BehaviorSpec({

        var fileSystem = FakeFileSystem()
        val downloader = mock<Downloader>()
        val models = mock<ModelsRepository>()

        beforeEach {
            fileSystem = FakeFileSystem()
            every { models.models } returns MutableStateFlow(listOf(fakeLlmModel()))
        }

        afterEach {
            resetAnswers(downloader, models)
            resetCalls(downloader, models)
        }

        fun useCase() =
            ObserveModelStatusesUseCase(
                models = models,
                downloader = downloader,
                modelFiles =
                    ModelFiles(
                        AppDirs(dataDir = "/data", modelsDir = "/models", cacheDir = "/cache"),
                        fileSystem,
                    ),
            )

        Given("the status map") {
            When("the downloader reports running progress") {
                Then("the model shows as downloading with rate and eta") {
                    val progress = MutableStateFlow<DownloadProgress?>(null)
                    every { downloader.observe(any()) } returns progress
                    val statuses =
                        useCase()
                            .invoke(
                                flowOf(Unit),
                            ).stateIn(detachedTestScope(), SharingStarted.Eagerly, emptyMap())
                    advanceUntilIdle()

                    progress.value =
                        DownloadProgress(
                            id = "model-1",
                            state = DownloadState.RUNNING,
                            downloadedBytes = 10,
                            totalBytes = 100,
                            bytesPerSecond = 5,
                            etaMillis = 18_000,
                        )
                    advanceUntilIdle()

                    statuses.value["model-1"] shouldBe
                        ModelStatus.Downloading(
                            downloadedBytes = 10,
                            totalBytes = 100,
                            bytesPerSecond = 5,
                            etaMillis = 18_000,
                        )
                }
            }

            When("the downloader reports success and the file landed") {
                Then("the model shows as downloaded") {
                    fileSystem.createDirectories("/models".toPath())
                    fileSystem.write("/models/m1.litertlm".toPath()) {}
                    val progress = MutableStateFlow<DownloadProgress?>(null)
                    every { downloader.observe(any()) } returns progress
                    val statuses =
                        useCase()
                            .invoke(
                                flowOf(Unit),
                            ).stateIn(detachedTestScope(), SharingStarted.Eagerly, emptyMap())
                    advanceUntilIdle()

                    progress.value = DownloadProgress(id = "model-1", state = DownloadState.SUCCEEDED)
                    advanceUntilIdle()

                    statuses.value["model-1"] shouldBe ModelStatus.Downloaded
                }
            }

            When("the file is deleted and a refresh is requested") {
                Then("the model returns to not downloaded") {
                    fileSystem.createDirectories("/models".toPath())
                    fileSystem.write("/models/m1.litertlm".toPath()) {}
                    every { downloader.observe(any()) } returns MutableStateFlow(null)
                    val refresh = MutableStateFlow(0)
                    val statuses =
                        useCase()
                            .invoke(
                                refresh.map { },
                            ).stateIn(detachedTestScope(), SharingStarted.Eagerly, emptyMap())
                    advanceUntilIdle()
                    statuses.value["model-1"] shouldBe ModelStatus.Downloaded

                    fileSystem.delete("/models/m1.litertlm".toPath())
                    refresh.update { it + 1 }
                    advanceUntilIdle()

                    statuses.value["model-1"] shouldBe ModelStatus.NotDownloaded
                }
            }
        }
    })
