package com.phamtunglam.lamity.unitTests.feature.history.domain

import com.phamtunglam.lamity.feature.chat.data.ConversationsRepository
import com.phamtunglam.lamity.feature.history.domain.ObserveConversationSummariesUseCase
import com.phamtunglam.lamity.fixtures.fakeConversation
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.resetAnswers
import dev.mokkery.resetCalls
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

class ObserveConversationSummariesUseCaseTest :
    BehaviorSpec({

        val conversations = mock<ConversationsRepository>()

        afterEach {
            resetAnswers(conversations)
            resetCalls(conversations)
        }

        fun createUseCase() = ObserveConversationSummariesUseCase(conversations)

        Given("stored conversations") {
            When("summaries are observed") {
                Then("each conversation is wrapped in a summary") {
                    every { conversations.conversations } returns
                        MutableStateFlow(
                            listOf(
                                fakeConversation(id = "conv-1", title = "First chat"),
                                fakeConversation(id = "conv-2", title = "Second chat"),
                            ),
                        )

                    val summaries = createUseCase()().first()

                    summaries.shouldHaveSize(2)
                    summaries.first().conversation.title shouldBe "First chat"
                }
            }
        }
    })
