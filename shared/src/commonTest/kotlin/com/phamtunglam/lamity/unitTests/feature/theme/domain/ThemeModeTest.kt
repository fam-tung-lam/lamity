package com.phamtunglam.lamity.feature.theme.domain

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class ThemeModeTest :
    BehaviorSpec({

        Given("a stored theme name") {
            When("it matches an enum constant exactly") {
                Then("it resolves to that theme") {
                    ThemeMode.fromStoredName("LIGHT") shouldBe ThemeMode.LIGHT
                    ThemeMode.fromStoredName("DARK") shouldBe ThemeMode.DARK
                    ThemeMode.fromStoredName("SYSTEM") shouldBe ThemeMode.SYSTEM
                }
            }
            When("it is null") {
                Then("it resolves to null") {
                    ThemeMode.fromStoredName(null) shouldBe null
                }
            }
            When("it is empty") {
                Then("it resolves to null") {
                    ThemeMode.fromStoredName("") shouldBe null
                }
            }
            When("it differs in casing or is unrecognized") {
                Then("it resolves to null") {
                    ThemeMode.fromStoredName("dark") shouldBe null
                    ThemeMode.fromStoredName("BOGUS") shouldBe null
                }
            }
        }
    })
