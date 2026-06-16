package com.phamtunglam.lamity.crashreporter.extensions

import com.phamtunglam.lamity.crashreporter.fixtures.fakeCrashReporterConfig
import com.phamtunglam.lamity.crashreporter.fixtures.fakeSentryOptions
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class LamityCrashReporterConfigExtTest :
    BehaviorSpec({

        Given("a fully populated config") {
            val config =
                fakeCrashReporterConfig(
                    dsn = "https://key@o0.ingest.sentry.io/1",
                    environment = "production",
                    release = "com.app@1.0.0+1",
                    debug = false,
                )

            When("applied to Sentry options") {
                val options = fakeSentryOptions()
                config.applyTo(options)

                Then("identity fields mirror the config") {
                    options.dsn shouldBe "https://key@o0.ingest.sentry.io/1"
                    options.environment shouldBe "production"
                    options.release shouldBe "com.app@1.0.0+1"
                    options.debug shouldBe false
                }

                Then("stack traces and threads are attached") {
                    options.attachStackTrace shouldBe true
                    options.attachThreads shouldBe true
                }

                Then("session tracking is enabled with a 30s interval") {
                    options.enableAutoSessionTracking shouldBe true
                    options.sessionTrackingIntervalMillis shouldBe 30_000L
                }

                Then("ANR tracking is enabled with a 5s timeout") {
                    options.isAnrEnabled shouldBe true
                    options.anrTimeoutIntervalMillis shouldBe 5_000L
                }

                Then("app-hang tracking is enabled with a 2s timeout") {
                    options.enableAppHangTracking shouldBe true
                    options.appHangTimeoutIntervalMillis shouldBe 2_000L
                }

                Then("watchdog termination tracking is enabled") {
                    options.enableWatchdogTerminationTracking shouldBe true
                }
            }
        }

        Given("a config with debug enabled") {
            val config = fakeCrashReporterConfig(debug = true)

            When("applied to options that start with debug disabled") {
                val options = fakeSentryOptions().apply { debug = false }
                config.applyTo(options)

                Then("debug is forwarded as true") {
                    options.debug shouldBe true
                }
            }
        }

        Given("a config without a release") {
            val config = fakeCrashReporterConfig(release = null)

            When("applied to options that already carry a release") {
                val options = fakeSentryOptions() // release = "stale-release"
                config.applyTo(options)

                Then("the existing release is left untouched") {
                    options.release shouldBe "stale-release"
                }
            }
        }
    })
