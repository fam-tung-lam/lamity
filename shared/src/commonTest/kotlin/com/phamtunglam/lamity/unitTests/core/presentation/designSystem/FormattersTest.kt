package com.phamtunglam.lamity.core.presentation.designSystem

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class FormattersTest :
    BehaviorSpec({

        Given("a byte count") {
            When("it is zero or negative") {
                Then("it renders a placeholder") {
                    formatBytes(0) shouldBe "?"
                    formatBytes(-1) shouldBe "?"
                }
            }
            When("it is at least a gigabyte") {
                Then("it renders gigabytes to one decimal place") {
                    formatBytes(1_610_612_736) shouldBe "1.5 GB"
                }
            }
            When("it is at least a megabyte") {
                Then("it renders whole megabytes") {
                    formatBytes(5_242_880) shouldBe "5 MB"
                }
            }
            When("it is below a megabyte") {
                Then("it renders whole kilobytes") {
                    formatBytes(2048) shouldBe "2 KB"
                }
            }
        }

        Given("a download fraction") {
            When("the total is unknown") {
                Then("it falls back to the received byte size") {
                    formatPercent(received = 2048, total = 0) shouldBe "2 KB"
                }
            }
            When("the transfer is partway through") {
                Then("it renders the percentage alongside both sizes") {
                    formatPercent(received = 1_048_576, total = 2_097_152) shouldBe "50% • 1 MB / 2 MB"
                }
            }
            When("the received bytes exceed the total") {
                Then("the percentage is clamped to 100") {
                    formatPercent(received = 3_145_728, total = 2_097_152) shouldBe "100% • 3 MB / 2 MB"
                }
            }
        }

        Given("a transfer speed") {
            When("it is non-positive") {
                Then("there is no speed to show") {
                    formatSpeed(0) shouldBe null
                }
            }
            When("it is positive") {
                Then("it renders bytes per second") {
                    formatSpeed(1024) shouldBe "1 KB/s"
                }
            }
        }

        Given("a remaining time") {
            When("it is non-positive") {
                Then("there is no eta to show") {
                    formatEta(0) shouldBe null
                }
            }
            When("it spans at least an hour") {
                Then("it renders hours and minutes") {
                    formatEta(3_660_000) shouldBe "1h 1m"
                }
            }
            When("it spans at least a minute") {
                Then("it renders minutes and seconds") {
                    formatEta(90_000) shouldBe "1m 30s"
                }
            }
            When("it is under a minute") {
                Then("it renders seconds") {
                    formatEta(5_000) shouldBe "5s"
                }
            }
        }
    })
