package com.phamtunglam.lamity.unitTests.feature.chat.domain.tools

import com.phamtunglam.lamity.core.presentation.confetti.ConfettiController
import com.phamtunglam.lamity.core.presentation.confetti.ConfettiStyle
import com.phamtunglam.lamity.feature.chat.domain.tools.ShowConfettiTool
import com.phamtunglam.lamity.fixtures.advanceUntilIdle
import com.phamtunglam.lamity.fixtures.detachedTestScope
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class ShowConfettiToolTest :
    BehaviorSpec({

        fun args(style: String?): JsonObject = buildJsonObject { style?.let { put("style", it) } }

        // Fires the tool against a real controller and returns its JSON result plus every burst the
        // controller emitted. The subscriber starts UNDISPATCHED so it is registered before the call.
        suspend fun fire(style: String?): Pair<JsonElement, List<ConfettiStyle>> {
            val controller = ConfettiController()
            val bursts = mutableListOf<ConfettiStyle>()
            val scope = detachedTestScope()
            scope.launch(start = CoroutineStart.UNDISPATCHED) { controller.events.collect { bursts += it } }

            val result = ShowConfettiTool(controller).execute(args(style))
            advanceUntilIdle()
            scope.cancel()
            return result to bursts
        }

        Given("the confetti tool") {
            When("invoked without a style") {
                Then("it celebrates with the festive style and reports success") {
                    val (result, bursts) = fire(style = null)

                    bursts shouldContainExactly listOf(ConfettiStyle.FESTIVE)
                    result.jsonObject["ok"]!!.jsonPrimitive.boolean shouldBe true
                    result.jsonObject["style"]!!.jsonPrimitive.content shouldBe "festive"
                }
            }

            When("invoked with an explicit style") {
                Then("it celebrates with that style") {
                    val (result, bursts) = fire(style = "explosion")

                    bursts shouldContainExactly listOf(ConfettiStyle.EXPLOSION)
                    result.jsonObject["style"]!!.jsonPrimitive.content shouldBe "explosion"
                }
            }

            When("invoked with an unknown style") {
                Then("it returns an error and does not celebrate") {
                    val (result, bursts) = fire(style = "sparkles")

                    bursts.shouldBeEmpty()
                    result.jsonObject["error"]!!.jsonPrimitive.content shouldBe
                        "style must be festive, rain or explosion"
                }
            }
        }
    })
