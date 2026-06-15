package com.phamtunglam.lamity.llm.unitTests.serialization

import com.phamtunglam.lamity.llm.model.Contents
import com.phamtunglam.lamity.llm.model.Message
import com.phamtunglam.lamity.llm.model.Role
import com.phamtunglam.lamity.llm.model.ToolCall
import com.phamtunglam.lamity.llm.serialization.LiteRtLmJson
import com.phamtunglam.lamity.llm.serialization.messageFromJson
import com.phamtunglam.lamity.llm.serialization.messageFromJsonString
import com.phamtunglam.lamity.llm.serialization.toJsonArrayString
import com.phamtunglam.lamity.llm.serialization.toJsonObject
import com.phamtunglam.lamity.llm.serialization.toJsonString
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

class MessageSerializationTest :
    BehaviorSpec({

        Given("messages with different fields") {
            When("a user text message is serialized") {
                Then("it carries the role and content but omits empty fields") {
                    val json = Message.user("hello").toJsonObject()

                    json["role"]!!.jsonPrimitive.content shouldBe "user"
                    json["content"]!!.jsonArray.size shouldBe 1
                    json.containsKey("tool_calls").shouldBeFalse()
                    json.containsKey("channels").shouldBeFalse()
                }
                Then("a message with empty contents omits the content field") {
                    Message(Role.Model).toJsonObject().containsKey("content").shouldBeFalse()
                }
                Then("channels are serialized when present") {
                    val json =
                        Message(Role.Model, Contents.text("hi"), channels = mapOf("thought" to "thinking"))
                            .toJsonObject()

                    json["channels"]!!.jsonObject["thought"]!!.jsonPrimitive.content shouldBe "thinking"
                }
                Then("tool calls are serialized when present") {
                    val json =
                        Message(Role.Model, toolCalls = listOf(ToolCall("get_time", JsonObject(emptyMap()))))
                            .toJsonObject()

                    json["tool_calls"]!!.jsonArray.size shouldBe 1
                }
            }
            When("each role is serialized") {
                Then("it uses the wire name") {
                    fun wireRole(message: Message) = message.toJsonObject()["role"]!!.jsonPrimitive.content

                    wireRole(Message.system("s")) shouldBe "system"
                    wireRole(Message.user("u")) shouldBe "user"
                    wireRole(Message.model("m")) shouldBe "model"
                    wireRole(Message.tool(Contents.text("t"))) shouldBe "tool"
                }
            }
        }

        Given("a serialized message") {
            When("it is parsed back") {
                Then("role, text and channels are restored") {
                    val original = Message(Role.User, Contents.text("hi"), channels = mapOf("k" to "v"))
                    val restored = messageFromJson(original.toJsonObject())

                    restored.role shouldBe Role.User
                    restored.text shouldBe "hi"
                    restored.channels shouldBe mapOf("k" to "v")
                }
                Then("the string round-trip preserves the message") {
                    val restored = messageFromJsonString(Message.system("be nice").toJsonString())

                    restored.role shouldBe Role.System
                    restored.text shouldBe "be nice"
                }
            }
            When("its role is unknown and it has unknown keys") {
                Then("the role falls back to user and unknown keys are ignored") {
                    val restored =
                        messageFromJsonString(
                            """{"role":"alien","content":[{"type":"text","text":"hi"}],"extra":"ignored"}""",
                        )

                    restored.role shouldBe Role.User
                    restored.text shouldBe "hi"
                }
            }
            When("a channel value is not a primitive") {
                Then("only primitive channel values are kept") {
                    val obj =
                        buildJsonObject {
                            put("role", "model")
                            putJsonObject("channels") {
                                put("thought", "thinking")
                                putJsonObject("nested") { put("x", "y") }
                            }
                        }

                    messageFromJson(obj).channels shouldBe mapOf("thought" to "thinking")
                }
            }
        }

        Given("a list of messages") {
            When("it is serialized to a JSON array string") {
                Then("it preserves order and roles") {
                    val json = listOf(Message.user("a"), Message.model("b")).toJsonArrayString()
                    val array = LiteRtLmJson.parseToJsonElement(json).jsonArray

                    array.size shouldBe 2
                    array[0].jsonObject["role"]!!.jsonPrimitive.content shouldBe "user"
                    array[1].jsonObject["role"]!!.jsonPrimitive.content shouldBe "model"
                }
            }
        }
    })
