package com.phamtunglam.lamity.feature.localization.domain

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class AppLocaleTest :
    BehaviorSpec({

        Given("a stored locale name") {
            When("it matches an enum constant exactly") {
                Then("it resolves to that locale") {
                    AppLocale.fromStoredName("ENGLISH") shouldBe AppLocale.ENGLISH
                    AppLocale.fromStoredName("SPANISH") shouldBe AppLocale.SPANISH
                }
            }
            When("it is null, empty, or differs in casing") {
                Then("it resolves to null") {
                    AppLocale.fromStoredName(null) shouldBe null
                    AppLocale.fromStoredName("") shouldBe null
                    AppLocale.fromStoredName("english") shouldBe null
                }
            }
        }

        Given("a device locale tag") {
            When("it carries a region and mixed casing") {
                Then("it matches on the primary language") {
                    AppLocale.fromDeviceLocaleTag("es-ES") shouldBe AppLocale.SPANISH
                    AppLocale.fromDeviceLocaleTag("EN_us") shouldBe AppLocale.ENGLISH
                    AppLocale.fromDeviceLocaleTag(" vi ") shouldBe AppLocale.VIETNAMESE
                }
            }
            When("its primary language is unsupported") {
                Then("it resolves to null") {
                    AppLocale.fromDeviceLocaleTag("fr-FR") shouldBe null
                }
            }
            When("it is null") {
                Then("it resolves to null") {
                    AppLocale.fromDeviceLocaleTag(null) shouldBe null
                }
            }
        }
    })
