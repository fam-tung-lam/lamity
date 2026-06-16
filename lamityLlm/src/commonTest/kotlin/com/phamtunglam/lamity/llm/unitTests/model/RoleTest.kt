package com.phamtunglam.lamity.llm.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class RoleTest :
    BehaviorSpec({

        Given("a JSON role name") {
            When("it is a known role name") {
                Then("it resolves each known role name") {
                    Role.fromJsonName("system") shouldBe Role.System
                    Role.fromJsonName("user") shouldBe Role.User
                    Role.fromJsonName("model") shouldBe Role.Model
                    Role.fromJsonName("tool") shouldBe Role.Tool
                }
            }
            When("it is the 'assistant' alias") {
                Then("it maps the assistant alias to Model") {
                    Role.fromJsonName("assistant") shouldBe Role.Model
                }
            }
            When("it is null or unknown") {
                Then("it falls back to User for null or unknown names") {
                    Role.fromJsonName(null) shouldBe Role.User
                    Role.fromJsonName("something-else") shouldBe Role.User
                }
            }
        }
    })
