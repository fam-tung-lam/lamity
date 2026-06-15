package com.phamtunglam.lamity.crashreporter.unitTests.models

import com.phamtunglam.lamity.crashreporter.fixtures.fakeCrashReporterConfig
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class LamityCrashReporterConfigTest :
    BehaviorSpec({

        // Empty and whitespace-only both count as blank; both must be rejected.
        val blanks = listOf("", "   ")

        Given("valid configuration values") {
            When("a config is constructed with every field populated") {
                val config =
                    fakeCrashReporterConfig(
                        dsn = "https://key@o0.ingest.sentry.io/1",
                        environment = "production",
                        release = "com.app@1.0.0+1",
                        debug = true,
                    )

                Then("it exposes the values verbatim") {
                    config.dsn shouldBe "https://key@o0.ingest.sentry.io/1"
                    config.environment shouldBe "production"
                    config.release shouldBe "com.app@1.0.0+1"
                    config.debug shouldBe true
                }
            }

            When("the optional fields are omitted") {
                val config = fakeCrashReporterConfig(release = null, debug = false)

                Then("release stays null and debug defaults to false") {
                    config.release shouldBe null
                    config.debug shouldBe false
                }
            }
        }

        Given("a blank DSN") {
            blanks.forEach { blank ->
                When("the DSN is \"$blank\"") {
                    Then("construction fails with the DSN message") {
                        val error =
                            shouldThrow<IllegalArgumentException> {
                                fakeCrashReporterConfig(dsn = blank)
                            }
                        error.message shouldBe "DSN must not be blank"
                    }
                }
            }
        }

        Given("a blank environment") {
            blanks.forEach { blank ->
                When("the environment is \"$blank\"") {
                    Then("construction fails with the environment message") {
                        val error =
                            shouldThrow<IllegalArgumentException> {
                                fakeCrashReporterConfig(environment = blank)
                            }
                        error.message shouldBe "Environment must not be blank"
                    }
                }
            }
        }

        Given("a release that is provided but blank") {
            blanks.forEach { blank ->
                When("the release is \"$blank\"") {
                    Then("construction fails with the release message") {
                        val error =
                            shouldThrow<IllegalArgumentException> {
                                fakeCrashReporterConfig(release = blank)
                            }
                        error.message shouldBe "Release must not be blank when provided"
                    }
                }
            }
        }

        Given("an absent release") {
            When("the release is null") {
                Then("construction succeeds") {
                    fakeCrashReporterConfig(release = null).release shouldBe null
                }
            }
        }
    })
