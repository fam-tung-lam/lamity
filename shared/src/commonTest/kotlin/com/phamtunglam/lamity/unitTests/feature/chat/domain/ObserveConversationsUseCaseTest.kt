package com.phamtunglam.lamity.unitTests.feature.chat.domain

import com.phamtunglam.lamity.feature.chat.data.ConversationsRepository
import com.phamtunglam.lamity.feature.chat.domain.ObserveConversationsUseCase
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

class ObserveConversationsUseCaseTest :
    BehaviorSpec({

        val conversations = mock<ConversationsRepository>()

        afterEach {
            resetAnswers(conversations)
            resetCalls(conversations)
        }

        fun createUseCase() = ObserveConversationsUseCase(conversations)

        Given("stored conversations") {
            When("conversations are observed") {
                Then("each stored conversation is emitted") {
                    every { conversations.conversations } returns
                        MutableStateFlow(
                            listOf(
                                fakeConversation(id = "conv-1", title = "First chat"),
                                fakeConversation(id = "conv-2", title = "Second chat"),
                            ),
                        )

                    val result = createUseCase()().first()

                    result.shouldHaveSize(2)
                    result.first().title shouldBe "First chat"
                }
            }
        }
    })
