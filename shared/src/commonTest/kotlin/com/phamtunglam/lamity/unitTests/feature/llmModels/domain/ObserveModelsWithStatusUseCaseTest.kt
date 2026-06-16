package com.phamtunglam.lamity.feature.llmModels.domain

import com.phamtunglam.lamity.core.domain.platform.AppDirs
import com.phamtunglam.lamity.downloader.Downloader
import com.phamtunglam.lamity.feature.llmModels.data.ModelFiles
import com.phamtunglam.lamity.feature.llmModels.data.ModelsRepository
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

// The status pipeline (ObserveModelStatusesUseCase + ModelFiles) is real here; this spec owns the
// catalog-join and the needs-token rule, while the status pipeline is owned by its own spec.
class ObserveModelsWithStatusUseCaseTest :
    BehaviorSpec({

        val models = mock<ModelsRepository>()
        val downloader = mock<Downloader>()
        var fileSystem = FakeFileSystem()

        beforeEach {
            fileSystem = FakeFileSystem()
            every { downloader.observe(any()) } returns MutableStateFlow(null)
        }

        afterEach {
            resetAnswers(models, downloader)
            resetCalls(models, downloader)
        }

        fun useCase(hfToken: String = ""): ObserveModelsWithStatusUseCase {
            val modelFiles =
                ModelFiles(AppDirs(dataDir = "/data", modelsDir = "/models", cacheDir = "/cache"), fileSystem)
            val observeStatuses = ObserveModelStatusesUseCase(models, downloader, modelFiles)
            return ObserveModelsWithStatusUseCase(models, observeStatuses, hfToken)
        }

        Given("a catalog model joined with its on-disk status") {
            When("the model file is absent") {
                Then("the model shows as not downloaded") {
                    every { models.models } returns MutableStateFlow(listOf(fakeLlmModel(id = "m1")))

                    val row = useCase().invoke(flowOf(Unit)).first().single()

                    row.status shouldBe ModelStatus.NotDownloaded
                }
            }
            When("the model file is present") {
                Then("the model shows as downloaded") {
                    fileSystem.createDirectories("/models".toPath())
                    fileSystem.write("/models/m1.litertlm".toPath()) {}
                    every { models.models } returns MutableStateFlow(listOf(fakeLlmModel(id = "m1")))

                    val row = useCase().invoke(flowOf(Unit)).first().single()

                    row.status shouldBe ModelStatus.Downloaded
                }
            }
        }

        Given("a gated model that requires a HuggingFace token") {
            When("the build has no token configured") {
                Then("the model needs a token") {
                    every { models.models } returns MutableStateFlow(listOf(fakeLlmModel(requiresAuth = true)))

                    val row = useCase(hfToken = "").invoke(flowOf(Unit)).first().single()

                    row.needsToken shouldBe true
                }
            }
            When("the build has a token configured") {
                Then("the model does not need a token") {
                    every { models.models } returns MutableStateFlow(listOf(fakeLlmModel(requiresAuth = true)))

                    val row = useCase(hfToken = "secret").invoke(flowOf(Unit)).first().single()

                    row.needsToken shouldBe false
                }
            }
        }

        Given("an ungated model") {
            When("the build has no token configured") {
                Then("the model does not need a token") {
                    every { models.models } returns MutableStateFlow(listOf(fakeLlmModel(requiresAuth = false)))

                    val row = useCase(hfToken = "").invoke(flowOf(Unit)).first().single()

                    row.needsToken shouldBe false
                }
            }
        }
    })
