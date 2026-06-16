package com.phamtunglam.lamity.llm.tool

import com.phamtunglam.lamity.llm.fixtures.fakeToolDescription
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

class ToolTest :
    BehaviorSpec({

        Given("a tool description") {
            When("it carries a function name") {
                Then("it returns the configured function name") {
                    val tool = mock<Tool>()
                    every { tool.getToolDescription() } returns fakeToolDescription("get_time")

                    tool.toolName() shouldBe "get_time"
                }
            }
            When("the function object is missing") {
                Then("it returns an empty string") {
                    val tool = mock<Tool>()
                    every { tool.getToolDescription() } returns buildJsonObject { put("type", "function") }

                    tool.toolName() shouldBe ""
                }
            }
            When("the function carries no name") {
                Then("it returns an empty string") {
                    val tool = mock<Tool>()
                    every { tool.getToolDescription() } returns
                        buildJsonObject { putJsonObject("function") { put("description", "x") } }

                    tool.toolName() shouldBe ""
                }
            }
        }
    })
