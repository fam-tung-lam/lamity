package com.phamtunglam.lamity.llm.unitTests.model

import com.phamtunglam.lamity.llm.model.Role
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class RoleTest :
    BehaviorSpec({

        Given("a JSON role name") {
            When("it is a known role name") {
                Then("fromJsonName returns the matching role") {
                    Role.fromJsonName("system") shouldBe Role.System
                    Role.fromJsonName("user") shouldBe Role.User
                    Role.fromJsonName("model") shouldBe Role.Model
                    Role.fromJsonName("tool") shouldBe Role.Tool
                }
            }
            When("it is the 'assistant' alias") {
                Then("fromJsonName maps it to Model") {
                    Role.fromJsonName("assistant") shouldBe Role.Model
                }
            }
            When("it is null or unknown") {
                Then("fromJsonName falls back to User") {
                    Role.fromJsonName(null) shouldBe Role.User
                    Role.fromJsonName("something-else") shouldBe Role.User
                }
            }
        }
    })
