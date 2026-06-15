package com.phamtunglam.lamity.llm.unitTests.serialization

import com.phamtunglam.lamity.llm.model.ToolCall
import com.phamtunglam.lamity.llm.serialization.toJson
import com.phamtunglam.lamity.llm.serialization.toolCallsFromJson
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

class ToolCallSerializationTest :
    BehaviorSpec({

        Given("a tool call with arguments") {
            When("it is serialized") {
                Then("it is wrapped in a function envelope with name and arguments") {
                    val args = buildJsonObject { put("city", "Paris") }
                    val json = ToolCall("get_weather", args).toJson()

                    json["type"]!!.jsonPrimitive.content shouldBe "function"
                    val function = json["function"]!!.jsonObject
                    function["name"]!!.jsonPrimitive.content shouldBe "get_weather"
                    function["arguments"] shouldBe args
                }
                Then("it round-trips back to an equal tool call") {
                    val args = buildJsonObject { put("city", "Paris") }
                    val call = ToolCall("get_weather", args)

                    toolCallsFromJson(buildJsonArray { add(call.toJson()) }) shouldBe listOf(call)
                }
            }
        }

        Given("an array of raw function-call objects") {
            When("an element is missing its function object") {
                Then("that element is skipped") {
                    val array = buildJsonArray { add(buildJsonObject { put("type", "function") }) }

                    toolCallsFromJson(array) shouldBe emptyList()
                }
            }
            When("a function is missing its name") {
                Then("that element is skipped") {
                    val array =
                        buildJsonArray {
                            add(buildJsonObject { putJsonObject("function") { put("arguments", buildJsonObject {}) } })
                        }

                    toolCallsFromJson(array) shouldBe emptyList()
                }
            }
            When("a function has no arguments") {
                Then("arguments default to an empty object") {
                    val array =
                        buildJsonArray { add(buildJsonObject { putJsonObject("function") { put("name", "ping") } }) }

                    toolCallsFromJson(array) shouldBe listOf(ToolCall("ping", JsonObject(emptyMap())))
                }
            }
            When("a function's arguments are not an object") {
                Then("arguments default to an empty object") {
                    val array =
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    putJsonObject("function") {
                                        put("name", "ping")
                                        put("arguments", JsonPrimitive("not-an-object"))
                                    }
                                },
                            )
                        }

                    toolCallsFromJson(array) shouldBe listOf(ToolCall("ping", JsonObject(emptyMap())))
                }
            }
        }
    })
