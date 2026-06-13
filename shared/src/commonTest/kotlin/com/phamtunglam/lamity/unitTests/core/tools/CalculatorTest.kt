package com.phamtunglam.lamity.unitTests.core.tools

import com.phamtunglam.lamity.core.tools.Calculator
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.math.PI

class CalculatorTest : BehaviorSpec({

    Given("arithmetic expressions") {
        When("operators with different precedence are mixed") {
            Then("it applies multiplication before addition") {
                Calculator.evaluate("2+3*4") shouldBe 14.0
            }
            Then("it lets parentheses override precedence") {
                Calculator.evaluate("(2+3)*4") shouldBe 20.0
            }
        }
        When("the power operator is chained") {
            Then("it associates to the right") {
                Calculator.evaluate("2^3^2") shouldBe 512.0
            }
        }
        When("a number uses scientific notation") {
            Then("it parses the exponent") {
                Calculator.evaluate("1.5e3") shouldBe 1500.0
            }
        }
    }

    Given("constants and named functions") {
        When("an expression references them") {
            Then("it resolves pi") {
                Calculator.evaluate("pi") shouldBe (PI plusOrMinus 1e-9)
            }
            Then("it applies single-argument functions") {
                Calculator.evaluate("sqrt(16)") shouldBe 4.0
            }
            Then("it applies two-argument functions") {
                Calculator.evaluate("pow(2, 10)") shouldBe 1024.0
            }
        }
    }

    Given("malformed input") {
        When("the expression is evaluated") {
            Then("it rejects a dangling operator") {
                shouldThrow<IllegalArgumentException> { Calculator.evaluate("2+") }
            }
            Then("it rejects unknown identifiers") {
                shouldThrow<IllegalArgumentException> { Calculator.evaluate("foo(1)") }
            }
            Then("it rejects trailing input") {
                shouldThrow<IllegalArgumentException> { Calculator.evaluate("1 2") }
            }
        }
    }
})
