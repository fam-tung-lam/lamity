package com.phamtunglam.lamity.logger.mappers

import co.touchlab.kermit.Severity
import com.phamtunglam.lamity.logger.logWriters.LamityLogWriter
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.verify
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.enum

class LogWriterMapperTest :
    BehaviorSpec({

        Given("a Lamity log writer adapted to a Kermit writer") {

            When("isLoggable is queried for any tag and severity") {
                Then("it delegates with the severity translated to Lamity and returns the result") {
                    checkAll(
                        Exhaustive.enum<Severity>(),
                        Arb.string(),
                        Arb.boolean(),
                    ) { severity, tag, loggable ->
                        val writer = mock<LamityLogWriter>()
                        every { writer.isLoggable(tag, severity.toLamity()) } returns loggable

                        writer.asKermitWriter().isLoggable(tag, severity) shouldBe loggable

                        verify { writer.isLoggable(tag, severity.toLamity()) }
                    }
                }
            }

            When("a record is logged for any severity, tag, message and throwable") {
                Then("it forwards the record unchanged, translating only the severity") {
                    val failure = IllegalStateException("boom")
                    checkAll(
                        Exhaustive.enum<Severity>(),
                        Arb.string(),
                        Arb.string(),
                        Arb.of(null, failure),
                    ) { severity, tag, message, throwable ->
                        val writer = mock<LamityLogWriter>()
                        every { writer.log(severity.toLamity(), message, tag, throwable) } returns Unit

                        writer.asKermitWriter().log(severity, message, tag, throwable)

                        verify { writer.log(severity.toLamity(), message, tag, throwable) }
                    }
                }
            }
        }
    })
