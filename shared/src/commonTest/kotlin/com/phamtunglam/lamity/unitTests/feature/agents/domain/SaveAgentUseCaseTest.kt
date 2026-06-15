package com.phamtunglam.lamity.unitTests.feature.agents.domain

import com.phamtunglam.lamity.feature.agents.data.AgentsRepository
import com.phamtunglam.lamity.feature.agents.domain.SaveAgentUseCase
import com.phamtunglam.lamity.fixtures.fakeAgent
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.resetAnswers
import dev.mokkery.resetCalls
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class SaveAgentUseCaseTest :
    BehaviorSpec({

        val agents = mock<AgentsRepository>()

        afterEach {
            resetAnswers(agents)
            resetCalls(agents)
        }

        Given("a valid agent name") {
            When("the agent is saved") {
                Then("it delegates to the repository and returns the agent") {
                    val agent = fakeAgent()
                    everySuspend {
                        agents.upsert(any(), any(), any(), any(), any(), any(), any(), any())
                    } returns agent

                    val saved =
                        SaveAgentUseCase(agents)(
                            id = null,
                            name = "Researcher",
                            description = "",
                            systemPrompt = "",
                            toolIds = emptyList(),
                            skillIds = emptyList(),
                            modelId = "model-1",
                            modelConfig = null,
                        )

                    saved shouldBe agent
                }
            }
        }

        Given("a blank agent name") {
            When("the agent is saved") {
                Then("nothing is persisted and null is returned") {
                    val saved =
                        SaveAgentUseCase(agents)(
                            id = null,
                            name = "   ",
                            description = "desc",
                            systemPrompt = "",
                            toolIds = emptyList(),
                            skillIds = emptyList(),
                            modelId = "model-1",
                            modelConfig = null,
                        )

                    saved.shouldBeNull()
                    verifySuspend(exactly(0)) {
                        agents.upsert(any(), any(), any(), any(), any(), any(), any(), any())
                    }
                }
            }
        }
    })
