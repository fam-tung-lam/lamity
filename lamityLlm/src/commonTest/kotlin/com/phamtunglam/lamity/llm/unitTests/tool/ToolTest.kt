package com.phamtunglam.lamity.llm.unitTests.tool

import com.phamtunglam.lamity.llm.tool.Tool
import com.phamtunglam.lamity.llm.tool.toolName
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

private class FakeTool(private val description: JsonObject) : Tool {
    override fun getToolDescription(): JsonObject = description

    override suspend fun execute(arguments: JsonObject): JsonElement = JsonNull
}

class ToolTest :
    BehaviorSpec({

        Given("a tool description") {
            When("it carries a function name") {
                Then("toolName returns that name") {
                    val tool =
                        FakeTool(
                            buildJsonObject {
                                put("type", "function")
                                putJsonObject("function") {
                                    put("name", "get_time")
                                    put("description", "Returns the current time")
                                }
                            },
                        )

                    tool.toolName() shouldBe "get_time"
                }
            }
            When("the function object is missing") {
                Then("toolName returns an empty string") {
                    FakeTool(buildJsonObject { put("type", "function") }).toolName() shouldBe ""
                }
            }
            When("the function has no name") {
                Then("toolName returns an empty string") {
                    val tool =
                        FakeTool(buildJsonObject { putJsonObject("function") { put("description", "x") } })

                    tool.toolName() shouldBe ""
                }
            }
        }
    })
