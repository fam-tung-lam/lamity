package com.phamtunglam.lamity.feature.localization.data

import com.phamtunglam.lamity.feature.localization.domain.AppLocale
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.resetAnswers
import dev.mokkery.resetCalls
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf

class AppLocaleRepositoryTest :
    BehaviorSpec({

        val store = mock<AppLocaleStore>()
        val deviceLocaleProvider = mock<DeviceLocaleProvider>()

        afterEach {
            resetAnswers(store, deviceLocaleProvider)
            resetCalls(store, deviceLocaleProvider)
        }

        fun repository() = AppLocaleRepository(store, deviceLocaleProvider)

        Given("a stored locale override") {
            When("the repository resolves the locale") {
                Then("it uses the stored override") {
                    every { store.locale } returns flowOf(AppLocale.SPANISH)

                    repository().locale.first() shouldBe AppLocale.SPANISH
                }
            }
        }

        Given("no stored override") {
            When("the device reports a supported language") {
                Then("it falls back to the device locale") {
                    every { store.locale } returns flowOf(null)
                    every { deviceLocaleProvider.currentLocaleTag() } returns "vi-VN"

                    repository().locale.first() shouldBe AppLocale.VIETNAMESE
                }
            }
            When("the device reports an unsupported language") {
                Then("it falls back to English") {
                    every { store.locale } returns flowOf(null)
                    every { deviceLocaleProvider.currentLocaleTag() } returns "fr-FR"

                    repository().locale.first() shouldBe AppLocale.ENGLISH
                }
            }
        }
    })
