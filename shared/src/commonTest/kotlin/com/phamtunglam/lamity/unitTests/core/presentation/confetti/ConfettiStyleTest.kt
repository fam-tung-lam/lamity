package com.phamtunglam.lamity.unitTests.core.presentation.confetti

import com.phamtunglam.lamity.core.presentation.confetti.ConfettiStyle
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class ConfettiStyleTest :
    BehaviorSpec({

        Given("a style name") {
            When("it matches a style ignoring case and surrounding space") {
                Then("it resolves the style") {
                    ConfettiStyle.fromName("festive") shouldBe ConfettiStyle.FESTIVE
                    ConfettiStyle.fromName("RAIN") shouldBe ConfettiStyle.RAIN
                    ConfettiStyle.fromName("  Explosion  ") shouldBe ConfettiStyle.EXPLOSION
                }
            }
            When("it is unknown or absent") {
                Then("it resolves to null") {
                    ConfettiStyle.fromName("sparkles").shouldBeNull()
                    ConfettiStyle.fromName(null).shouldBeNull()
                }
            }
        }
    })
