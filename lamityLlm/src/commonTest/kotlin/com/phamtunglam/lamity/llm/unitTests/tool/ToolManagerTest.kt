package com.phamtunglam.lamity.llm.unitTests.tool

import com.phamtunglam.lamity.llm.LiteRtLmException
import com.phamtunglam.lamity.llm.tool.Tool
import com.phamtunglam.lamity.llm.tool.ToolManager
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

private class StubTool(private val description: JsonObject, private val result: JsonElement = JsonNull) : Tool {
    override fun getToolDescription(): JsonObject = description

    override suspend fun execute(arguments: JsonObject): JsonElement = result
}

private fun toolDescription(name: String): JsonObject =
    buildJsonObject {
        put("type", "function")
        putJsonObject("function") { put("name", name) }
    }

class ToolManagerTest :
    BehaviorSpec({

        Given("a ToolManager") {
            When("a tool description carries no function name") {
                Then("construction throws LiteRtLmException") {
                    shouldThrow<LiteRtLmException> {
                        ToolManager(listOf(StubTool(buildJsonObject { put("type", "function") })))
                    }
                }
            }
            When("a registered tool is executed by name") {
                Then("it returns the tool result") {
                    val manager = ToolManager(listOf(StubTool(toolDescription("echo"), JsonPrimitive("ok"))))

                    manager.execute("echo", buildJsonObject {}) shouldBe JsonPrimitive("ok")
                }
            }
            When("an unknown tool name is executed") {
                Then("it throws LiteRtLmException") {
                    val manager = ToolManager(listOf(StubTool(toolDescription("echo"))))

                    shouldThrow<LiteRtLmException> { manager.execute("missing", buildJsonObject {}) }
                }
            }
        }
    })
