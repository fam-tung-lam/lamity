package com.phamtunglam.lamity.unitTests.feature.history.domain

import com.phamtunglam.lamity.feature.chat.data.ConversationsRepository
import com.phamtunglam.lamity.feature.history.domain.ObserveConversationSummariesUseCase
import com.phamtunglam.lamity.feature.models.data.ModelsRepository
import com.phamtunglam.lamity.feature.studio.data.AgentsRepository
import com.phamtunglam.lamity.fixtures.fakeAgent
import com.phamtunglam.lamity.fixtures.fakeConversation
import com.phamtunglam.lamity.fixtures.fakeLlmModel
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.resetAnswers
import dev.mokkery.resetCalls
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

class ObserveConversationSummariesUseCaseTest :
    BehaviorSpec({

        val conversations = mock<ConversationsRepository>()
        val agents = mock<AgentsRepository>()
        val models = mock<ModelsRepository>()

        afterEach {
            resetAnswers(conversations, agents, models)
            resetCalls(conversations, agents, models)
        }

        fun createUseCase() = ObserveConversationSummariesUseCase(conversations, agents, models)

        Given("a conversation whose agent and model exist") {
            When("summaries are observed") {
                Then("it joins the agent and model display names") {
                    every { conversations.conversations } returns
                        MutableStateFlow(
                            listOf(fakeConversation(agentId = "agent-1", modelId = "model-1")),
                        )
                    every { agents.agents } returns MutableStateFlow(listOf(fakeAgent(id = "agent-1")))
                    every { models.models } returns MutableStateFlow(listOf(fakeLlmModel(id = "model-1")))

                    val summaries = createUseCase()().first()

                    summaries.single().agentName shouldBe "Researcher"
                    summaries.single().modelName shouldBe "Model model-1"
                }
            }
        }

        Given("a conversation referencing a deleted agent and unknown model") {
            When("summaries are observed") {
                Then("the agent name is null and the model id is the fallback name") {
                    every { conversations.conversations } returns
                        MutableStateFlow(
                            listOf(fakeConversation(agentId = "gone", modelId = "unknown-model")),
                        )
                    every { agents.agents } returns MutableStateFlow(emptyList())
                    every { models.models } returns MutableStateFlow(emptyList())

                    val summaries = createUseCase()().first()

                    summaries.single().agentName.shouldBeNull()
                    summaries.single().modelName shouldBe "unknown-model"
                }
            }
        }
    })
