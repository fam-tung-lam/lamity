package com.phamtunglam.lamity.unitTests.feature.models.domain

import com.phamtunglam.lamity.feature.models.data.ModelsRepository
import com.phamtunglam.lamity.feature.models.domain.LlmBackend
import com.phamtunglam.lamity.feature.models.domain.ModelConfig
import com.phamtunglam.lamity.feature.models.domain.SaveModelConfigUseCase
import com.phamtunglam.lamity.fixtures.fakeLlmModel
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.resetAnswers
import dev.mokkery.resetCalls
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class SaveModelConfigUseCaseTest : BehaviorSpec({

    val models = mock<ModelsRepository>()

    afterEach {
        resetAnswers(models)
        resetCalls(models)
    }

    Given("a model in the catalog") {
        When("a raw slider config is saved") {
            Then("the values are snapped to their valid grid") {
                every { models.byId("model-1") } returns fakeLlmModel()
                var saved: ModelConfig? = null
                everySuspend { models.updateConfig(any(), any()) } calls {
                    (_: String, config: ModelConfig) ->
                    saved = config
                }

                SaveModelConfigUseCase(models)(
                    modelId = "model-1",
                    backend = LlmBackend.CPU,
                    maxTokens = 1000f,
                    topK = 39.6f,
                    topP = 0.949f,
                    temperature = 0.812f,
                )

                saved!!.backend shouldBe LlmBackend.CPU
                saved!!.maxTokens shouldBe 1024
                saved!!.topK shouldBe 40
                saved!!.topP shouldBe 0.95
                saved!!.temperature shouldBe 0.81
            }
        }
    }

    Given("an unknown model id") {
        When("a config is saved") {
            Then("nothing is persisted") {
                every { models.byId("missing") } returns null

                SaveModelConfigUseCase(models)(
                    modelId = "missing",
                    backend = LlmBackend.GPU,
                    maxTokens = 2048f,
                    topK = 40f,
                    topP = 0.95f,
                    temperature = 0.8f,
                )

                verifySuspend(exactly(0)) { models.updateConfig(any(), any()) }
            }
        }
    }
})
