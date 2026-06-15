package com.phamtunglam.lamity.llm.unitTests.serialization

import com.phamtunglam.lamity.llm.model.Content
import com.phamtunglam.lamity.llm.model.Contents
import com.phamtunglam.lamity.llm.serialization.contentFromJson
import com.phamtunglam.lamity.llm.serialization.toJson
import com.phamtunglam.lamity.llm.serialization.toJsonArray
import com.phamtunglam.lamity.llm.serialization.toJsonArrayString
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class ContentSerializationTest :
    BehaviorSpec({

        Given("a text content") {
            When("it is serialized and parsed back") {
                Then("it round-trips through a text object") {
                    val json = Content.Text("hello").toJson()

                    json["type"]!!.jsonPrimitive.content shouldBe "text"
                    json["text"]!!.jsonPrimitive.content shouldBe "hello"

                    val decoded = contentFromJson(json).shouldBeInstanceOf<Content.Text>()
                    decoded.text shouldBe "hello"
                }
                Then("a text object with no text field decodes to an empty string") {
                    val decoded =
                        contentFromJson(buildJsonObject { put("type", "text") })
                            .shouldBeInstanceOf<Content.Text>()
                    decoded.text shouldBe ""
                }
            }
        }

        Given("binary image and audio content") {
            When("image bytes are serialized") {
                Then("they are base64-encoded under blob and decode back to the same bytes") {
                    val json = Content.ImageBytes(byteArrayOf(1, 2, 3, 4)).toJson()

                    json["type"]!!.jsonPrimitive.content shouldBe "image"
                    json.containsKey("blob").shouldBeTrue()

                    val decoded = contentFromJson(json).shouldBeInstanceOf<Content.ImageBytes>()
                    decoded.bytes.toList() shouldBe byteArrayOf(1, 2, 3, 4).toList()
                }
            }
            When("audio bytes are serialized") {
                Then("they are base64-encoded under blob and decode back to the same bytes") {
                    val json = Content.AudioBytes(byteArrayOf(10, 20, 30)).toJson()

                    json["type"]!!.jsonPrimitive.content shouldBe "audio"

                    val decoded = contentFromJson(json).shouldBeInstanceOf<Content.AudioBytes>()
                    decoded.bytes.toList() shouldBe byteArrayOf(10, 20, 30).toList()
                }
            }
        }

        Given("file-referenced image and audio content") {
            When("an image file is serialized") {
                Then("it is stored as a path and decodes to an image file") {
                    val json = Content.ImageFile("/tmp/a.png").toJson()

                    json["type"]!!.jsonPrimitive.content shouldBe "image"
                    json["path"]!!.jsonPrimitive.content shouldBe "/tmp/a.png"

                    contentFromJson(json).shouldBeInstanceOf<Content.ImageFile>().path shouldBe "/tmp/a.png"
                }
            }
            When("an audio file is serialized") {
                Then("it is stored as a path and decodes to an audio file") {
                    val json = Content.AudioFile("/tmp/a.wav").toJson()

                    contentFromJson(json).shouldBeInstanceOf<Content.AudioFile>().path shouldBe "/tmp/a.wav"
                }
            }
        }

        Given("a tool response") {
            When("it carries a JSON payload") {
                Then("it round-trips the name and the response element") {
                    val payload = buildJsonObject { put("ok", true) }
                    val json = Content.ToolResponse("get_time", payload).toJson()

                    json["type"]!!.jsonPrimitive.content shouldBe "tool_response"
                    json["name"]!!.jsonPrimitive.content shouldBe "get_time"
                    json["response"] shouldBe payload

                    val decoded = contentFromJson(json).shouldBeInstanceOf<Content.ToolResponse>()
                    decoded.name shouldBe "get_time"
                    decoded.response shouldBe payload
                }
                Then("a null response is serialized as JSON null and decodes back to JSON null") {
                    val json = Content.ToolResponse("noop", null).toJson()

                    json["response"] shouldBe JsonNull

                    val decoded = contentFromJson(json).shouldBeInstanceOf<Content.ToolResponse>()
                    decoded.response shouldBe JsonNull
                }
            }
        }

        Given("unrecognized or incomplete content objects") {
            When("they are parsed") {
                Then("an unknown type yields null") {
                    contentFromJson(buildJsonObject { put("type", "video") }).shouldBeNull()
                }
                Then("a missing type yields null") {
                    contentFromJson(buildJsonObject { put("text", "hi") }).shouldBeNull()
                }
                Then("an image with neither blob nor path yields null") {
                    contentFromJson(buildJsonObject { put("type", "image") }).shouldBeNull()
                }
            }
        }

        Given("a collection of contents") {
            When("it is serialized to a JSON array") {
                Then("it preserves order and exposes the same string form") {
                    val contents = Contents.of(Content.Text("a"), Content.Text("b"))
                    val array = contents.toJsonArray()

                    array.size shouldBe 2
                    array[0].jsonObject["text"]!!.jsonPrimitive.content shouldBe "a"
                    array[1].jsonObject["text"]!!.jsonPrimitive.content shouldBe "b"
                    contents.toJsonArrayString() shouldBe array.toString()
                }
                Then("an empty collection serializes to an empty array") {
                    Contents.empty.toJsonArray().shouldBeEmpty()
                    Contents.empty.toJsonArrayString() shouldBe "[]"
                }
            }
        }

        Given("a serialized text object with an extra unknown field") {
            When("it is parsed") {
                Then("the unknown field is ignored") {
                    val json =
                        buildJsonObject {
                            put("type", "text")
                            put("text", "hi")
                            put("unknown", "ignored")
                        }

                    contentFromJson(json).shouldBeInstanceOf<Content.Text>().text shouldBe "hi"
                    json.containsKey("blob").shouldBeFalse()
                }
            }
        }
    })
