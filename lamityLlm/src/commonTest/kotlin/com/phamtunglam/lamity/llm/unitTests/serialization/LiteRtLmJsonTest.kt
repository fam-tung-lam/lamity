package com.phamtunglam.lamity.llm.unitTests.serialization

import com.phamtunglam.lamity.llm.serialization.LiteRtLmJson
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class LiteRtLmJsonTest :
    BehaviorSpec({

        Given("the LiteRT-LM JSON configuration") {
            When("a lenient document with unquoted tokens is parsed") {
                Then("it parses without requiring strict quoting") {
                    val element = LiteRtLmJson.parseToJsonElement("{role: model}")

                    element.jsonObject["role"]!!.jsonPrimitive.content shouldBe "model"
                }
            }
            When("a strictly quoted document is parsed") {
                Then("it parses normally") {
                    val element = LiteRtLmJson.parseToJsonElement("""{"role":"user"}""")

                    element.jsonObject["role"]!!.jsonPrimitive.content shouldBe "user"
                }
            }
        }
    })
