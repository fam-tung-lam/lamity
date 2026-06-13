package com.phamtunglam.lamity.unitTests.core.logging

import com.phamtunglam.lamity.core.logging.CrashReportingLogWriter
import com.phamtunglam.lamity.crashreporter.CrashReporter
import com.phamtunglam.lamity.logger.models.LamityLogSeverity
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.resetAnswers
import dev.mokkery.resetCalls
import dev.mokkery.verify
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class CrashReportingLogWriterTest : BehaviorSpec({

    val reporter = mock<CrashReporter>()

    afterEach {
        resetAnswers(reporter)
        resetCalls(reporter)
    }

    Given("a crash reporting log writer") {
        When("a record meets the minimum severity") {
            Then("it forwards the record as a breadcrumb") {
                every { reporter.isEnabled } returns true
                every { reporter.addBreadcrumb("Chat", "engine ready") } returns Unit
                val writer = CrashReportingLogWriter(reporter)

                writer.log(LamityLogSeverity.Info, "engine ready", "Chat", null)

                verify { reporter.addBreadcrumb("Chat", "engine ready") }
            }
        }

        When("a record is below the minimum severity") {
            Then("it is not loggable") {
                every { reporter.isEnabled } returns true
                val writer = CrashReportingLogWriter(reporter, minSeverity = LamityLogSeverity.Warn)

                writer.isLoggable("Chat", LamityLogSeverity.Info) shouldBe false
            }
        }

        When("an error record carries a throwable") {
            Then("it captures the exception tagged with the log tag") {
                every { reporter.isEnabled } returns true
                val failure = IllegalStateException("boom")
                every { reporter.addBreadcrumb("Chat", "it broke") } returns Unit
                every {
                    reporter.captureException(failure, mapOf("log.tag" to "Chat"))
                } returns Unit
                val writer = CrashReportingLogWriter(reporter)

                writer.log(LamityLogSeverity.Error, "it broke", "Chat", failure)

                verify { reporter.captureException(failure, mapOf("log.tag" to "Chat")) }
            }
        }

        When("the reporter is disabled") {
            Then("nothing is loggable") {
                every { reporter.isEnabled } returns false
                val writer = CrashReportingLogWriter(reporter)

                writer.isLoggable("Chat", LamityLogSeverity.Error) shouldBe false
            }
        }
    }
})
