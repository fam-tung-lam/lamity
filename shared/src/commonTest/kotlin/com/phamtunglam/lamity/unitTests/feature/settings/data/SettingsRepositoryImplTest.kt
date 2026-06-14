package com.phamtunglam.lamity.unitTests.feature.settings.data

import com.phamtunglam.lamity.db.daos.SettingsDao
import com.phamtunglam.lamity.db.entities.SettingsEntity
import com.phamtunglam.lamity.feature.settings.data.SettingsRepository
import com.phamtunglam.lamity.feature.settings.data.SettingsRepositoryImpl
import com.phamtunglam.lamity.feature.settings.domain.ThemeMode
import com.phamtunglam.lamity.fixtures.advanceUntilIdle
import com.phamtunglam.lamity.fixtures.detachedTestScope
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.resetAnswers
import dev.mokkery.resetCalls
import dev.mokkery.verifySuspend
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow

private fun fakeSettingsEntity(
    themeMode: String = "SYSTEM",
    language: String = "en",
    toolEnabledJson: String = "{}",
    lastModelId: String? = null,
    lastAgentId: String? = null,
) = SettingsEntity(
    id = 0,
    themeMode = themeMode,
    language = language,
    toolEnabledJson = toolEnabledJson,
    lastModelId = lastModelId,
    lastAgentId = lastAgentId,
)

class SettingsRepositoryImplTest : BehaviorSpec({

    val dao = mock<SettingsDao>()

    afterEach {
        resetAnswers(dao)
        resetCalls(dao)
    }

    // The database is the single source of truth: the mocked DAO is backed by
    // a StateFlow that writes update and the repository observes.
    suspend fun createRepository(initial: SettingsEntity?): SettingsRepository {
        val backing = MutableStateFlow(initial)
        every { dao.observe() } returns backing
        everySuspend { dao.get() } calls { backing.value }
        everySuspend { dao.upsert(any()) } calls { (entity: SettingsEntity) ->
            backing.value = entity
        }
        val repository = SettingsRepositoryImpl(dao, detachedTestScope())
        repository.awaitLoaded()
        return repository
    }

    Given("a persisted settings row") {
        When("the repository loads") {
            Then("it exposes the stored settings") {
                val repository = createRepository(
                    fakeSettingsEntity(
                        themeMode = "DARK",
                        language = "vi",
                        toolEnabledJson = """{"calculate":false}""",
                        lastModelId = "m1",
                    ),
                )

                repository.value.themeMode shouldBe ThemeMode.DARK
                repository.value.language shouldBe "vi"
                repository.value.toolEnabled shouldBe mapOf("calculate" to false)
            }
            Then("it treats tools without a stored toggle as enabled") {
                val repository = createRepository(
                    fakeSettingsEntity(toolEnabledJson = """{"calculate":false}"""),
                )

                repository.isToolEnabled("calculate") shouldBe false
                repository.isToolEnabled("unknown_tool") shouldBe true
            }
        }
    }

    Given("no persisted settings") {
        When("a setting is updated") {
            Then("the change surfaces back through the database flow") {
                val repository = createRepository(null)

                repository.setThemeMode(ThemeMode.LIGHT)
                advanceUntilIdle()

                repository.value.themeMode shouldBe ThemeMode.LIGHT
            }
            Then("it writes the new row through to the database") {
                val repository = createRepository(null)

                repository.setLanguage("vi")
                advanceUntilIdle()

                verifySuspend { dao.upsert(any()) }
            }
            Then("successive updates never overwrite each other") {
                val repository = createRepository(null)

                repository.setLanguage("vi")
                repository.setThemeMode(ThemeMode.DARK)
                advanceUntilIdle()

                repository.value.language shouldBe "vi"
                repository.value.themeMode shouldBe ThemeMode.DARK
            }
        }
    }
})
