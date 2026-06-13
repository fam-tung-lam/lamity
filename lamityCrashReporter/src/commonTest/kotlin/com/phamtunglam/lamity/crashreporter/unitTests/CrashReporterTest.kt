package com.phamtunglam.lamity.crashreporter.unitTests

import co.touchlab.kermit.Severity
import com.phamtunglam.lamity.crashreporter.CrashBreadcrumbWriter
import com.phamtunglam.lamity.crashreporter.CrashReporter
import com.phamtunglam.lamity.crashreporter.CrashReporterConfig
import com.phamtunglam.lamity.crashreporter.sentryCrashReporter
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.resetAnswers
import dev.mokkery.resetCalls
import dev.mokkery.verify
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.shouldBe

class CrashReporterTest : BehaviorSpec({

    val reporter = mock<CrashReporter>()

    afterEach {
        resetAnswers(reporter)
        resetCalls(reporter)
    }

    Given("a blank DSN") {
        When("the Sentry reporter is created") {
            Then("it stays disabled without touching the Sentry SDK") {
                sentryCrashReporter(CrashReporterConfig(dsn = "")).isEnabled.shouldBeFalse()
            }
        }
    }

    Given("a crash breadcrumb writer") {
        When("a record meets the minimum severity") {
            Then("it forwards the record as a breadcrumb") {
                every { reporter.isEnabled } returns true
                every { reporter.addBreadcrumb("Chat", "engine ready") } returns Unit
                val writer = CrashBreadcrumbWriter(reporter)

                writer.log(Severity.Info, "engine ready", "Chat", null)

                verify { reporter.addBreadcrumb("Chat", "engine ready") }
            }
        }

        When("a record is below the minimum severity") {
            Then("it is not loggable") {
                every { reporter.isEnabled } returns true
                val writer = CrashBreadcrumbWriter(reporter, minSeverity = Severity.Warn)

                writer.isLoggable("Chat", Severity.Info) shouldBe false
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
                val writer = CrashBreadcrumbWriter(reporter)

                writer.log(Severity.Error, "it broke", "Chat", failure)

                verify { reporter.captureException(failure, mapOf("log.tag" to "Chat")) }
            }
        }

        When("the reporter is disabled") {
            Then("nothing is loggable") {
                every { reporter.isEnabled } returns false
                val writer = CrashBreadcrumbWriter(reporter)

                writer.isLoggable("Chat", Severity.Error) shouldBe false
            }
        }
    }
})
