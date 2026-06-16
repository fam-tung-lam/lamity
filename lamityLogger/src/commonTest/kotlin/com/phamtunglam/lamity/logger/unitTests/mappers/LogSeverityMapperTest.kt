package com.phamtunglam.lamity.logger.mappers

import co.touchlab.kermit.Severity
import com.phamtunglam.lamity.logger.models.LamityLogSeverity
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Exhaustive
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.collection

class LogSeverityMapperTest :
    BehaviorSpec({

        // The canonical 1:1 correspondence between the two severity scales.
        val pairs =
            listOf(
                Severity.Verbose to LamityLogSeverity.Verbose,
                Severity.Debug to LamityLogSeverity.Debug,
                Severity.Info to LamityLogSeverity.Info,
                Severity.Warn to LamityLogSeverity.Warn,
                Severity.Error to LamityLogSeverity.Error,
                Severity.Assert to LamityLogSeverity.Assert,
            )

        Given("a Kermit severity") {
            When("mapped to Lamity") {
                Then("every value becomes its canonical Lamity counterpart") {
                    checkAll(Exhaustive.collection(pairs)) { (kermit, lamity) ->
                        kermit.toLamity() shouldBe lamity
                    }
                }
            }
        }

        Given("a Lamity severity") {
            When("mapped to Kermit") {
                Then("every value becomes its canonical Kermit counterpart") {
                    checkAll(Exhaustive.collection(pairs)) { (kermit, lamity) ->
                        lamity.toKermit() shouldBe kermit
                    }
                }
            }
        }
    })
