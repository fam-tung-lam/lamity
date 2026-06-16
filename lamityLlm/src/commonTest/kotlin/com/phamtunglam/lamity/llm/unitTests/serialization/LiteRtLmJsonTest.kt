package com.phamtunglam.lamity.llm.serialization

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
        }
    })
