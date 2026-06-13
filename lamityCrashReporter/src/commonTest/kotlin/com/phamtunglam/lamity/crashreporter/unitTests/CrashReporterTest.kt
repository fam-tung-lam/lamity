package com.phamtunglam.lamity.crashreporter.unitTests

import com.phamtunglam.lamity.crashreporter.CrashReporterConfig
import com.phamtunglam.lamity.crashreporter.sentryCrashReporter
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.booleans.shouldBeFalse

class CrashReporterTest : BehaviorSpec({

    Given("a blank DSN") {
        When("the Sentry reporter is created") {
            Then("it stays disabled without touching the Sentry SDK") {
                sentryCrashReporter(CrashReporterConfig(dsn = "")).isEnabled.shouldBeFalse()
            }
        }
    }
})
