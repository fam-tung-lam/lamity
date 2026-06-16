package com.phamtunglam.lamity.llm.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class ContentsTest :
    BehaviorSpec({

        Given("a mix of text and non-text content") {
            When("its text is read") {
                Then("it joins only the text parts, space separated") {
                    val contents =
                        Contents.of(
                            Content.Text("hello"),
                            Content.ToolResponse("tool", null),
                            Content.Text("world"),
                        )

                    contents.text shouldBe "hello world"
                }
            }
        }

        Given("empty contents") {
            When("it is inspected") {
                Then("it reports empty with blank text") {
                    Contents.empty.isEmpty shouldBe true
                    Contents.empty.text shouldBe ""
                }
            }
        }
    })
