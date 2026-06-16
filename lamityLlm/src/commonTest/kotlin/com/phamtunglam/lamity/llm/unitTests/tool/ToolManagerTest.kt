package com.phamtunglam.lamity.llm.tool

import com.phamtunglam.lamity.llm.LiteRtLmException
import com.phamtunglam.lamity.llm.fixtures.fakeToolDescription
import com.phamtunglam.lamity.llm.model.Content
import com.phamtunglam.lamity.llm.model.Role
import com.phamtunglam.lamity.llm.model.ToolCall
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ToolManagerTest :
    BehaviorSpec({

        Given("a ToolManager") {
            When("a tool description carries no function name") {
                Then("construction throws LiteRtLmException") {
                    val nameless = mock<Tool>()
                    every { nameless.getToolDescription() } returns buildJsonObject { put("type", "function") }

                    shouldThrow<LiteRtLmException> { ToolManager(listOf(nameless)) }
                }
            }
            When("a registered tool is executed by name") {
                Then("it returns the tool result") {
                    val echo = mock<Tool>()
                    every { echo.getToolDescription() } returns fakeToolDescription("echo")
                    everySuspend { echo.execute(any()) } returns JsonPrimitive("ok")
                    val manager = ToolManager(listOf(echo))

                    manager.execute("echo", buildJsonObject {}) shouldBe JsonPrimitive("ok")
                }
            }
            When("an unknown tool name is executed") {
                Then("it throws LiteRtLmException") {
                    val echo = mock<Tool>()
                    every { echo.getToolDescription() } returns fakeToolDescription("echo")
                    val manager = ToolManager(listOf(echo))

                    shouldThrow<LiteRtLmException> { manager.execute("missing", buildJsonObject {}) }
                }
            }
        }

        Given("a batch of tool calls") {
            When("a tool succeeds") {
                Then("it assembles a single tool-response message carrying the result") {
                    val echo = mock<Tool>()
                    every { echo.getToolDescription() } returns fakeToolDescription("echo")
                    everySuspend { echo.execute(any()) } returns JsonPrimitive("ok")
                    val manager = ToolManager(listOf(echo))

                    val message = manager.handleToolCalls(listOf(ToolCall("echo", buildJsonObject {})))

                    message.role shouldBe Role.Tool
                    val response = message.contents.values.single() as Content.ToolResponse
                    response.name shouldBe "echo"
                    response.response shouldBe JsonPrimitive("ok")
                }
            }
            When("a tool fails with a LiteRtLmException") {
                Then("the exception propagates unchanged") {
                    val failing = mock<Tool>()
                    every { failing.getToolDescription() } returns fakeToolDescription("failing")
                    everySuspend { failing.execute(any()) } throws LiteRtLmException("native boom")
                    val manager = ToolManager(listOf(failing))

                    shouldThrow<LiteRtLmException> {
                        manager.handleToolCalls(listOf(ToolCall("failing", buildJsonObject {})))
                    }
                }
            }
            When("a tool fails with an unexpected exception") {
                Then("it is wrapped as a LiteRtLmException") {
                    val failing = mock<Tool>()
                    every { failing.getToolDescription() } returns fakeToolDescription("failing")
                    everySuspend { failing.execute(any()) } throws IllegalStateException("kaboom")
                    val manager = ToolManager(listOf(failing))

                    shouldThrow<LiteRtLmException> {
                        manager.handleToolCalls(listOf(ToolCall("failing", buildJsonObject {})))
                    }
                }
            }
        }
    })
