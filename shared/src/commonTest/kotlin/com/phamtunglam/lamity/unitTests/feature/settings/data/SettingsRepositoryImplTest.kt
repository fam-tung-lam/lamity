package com.phamtunglam.lamity.unitTests.feature.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.phamtunglam.lamity.feature.settings.data.SettingsRepository
import com.phamtunglam.lamity.feature.settings.data.SettingsRepositoryImpl
import com.phamtunglam.lamity.feature.settings.domain.ThemeMode
import com.phamtunglam.lamity.fixtures.advanceUntilIdle
import com.phamtunglam.lamity.fixtures.detachedTestScope
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [DataStore] mirroring preferences DataStore read-modify-write semantics. */
private class FakePreferencesDataStore(initial: Preferences) : DataStore<Preferences> {
    private val state = MutableStateFlow(initial)
    override val data: Flow<Preferences> = state
    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}

private fun storedPreferences(
    themeMode: String? = null,
    toolEnabledJson: String? = null,
    lastModelId: String? = null,
): Preferences = mutablePreferencesOf().apply {
    themeMode?.let { this[stringPreferencesKey("theme_mode")] = it }
    toolEnabledJson?.let { this[stringPreferencesKey("tool_enabled")] = it }
    lastModelId?.let { this[stringPreferencesKey("last_model_id")] = it }
}

class SettingsRepositoryImplTest : BehaviorSpec({

    // The preferences DataStore is the single source of truth: the in-memory
    // fake persists writes and the repository observes them back.
    suspend fun createRepository(initial: Preferences = emptyPreferences()): SettingsRepository {
        val repository = SettingsRepositoryImpl(FakePreferencesDataStore(initial), detachedTestScope())
        repository.awaitLoaded()
        return repository
    }

    Given("persisted settings") {
        When("the repository loads") {
            Then("it exposes the stored settings") {
                val repository = createRepository(
                    storedPreferences(
                        themeMode = "DARK",
                        toolEnabledJson = """{"calculate":false}""",
                        lastModelId = "m1",
                    ),
                )

                repository.value.themeMode shouldBe ThemeMode.DARK
                repository.value.toolEnabled shouldBe mapOf("calculate" to false)
                repository.value.lastModelId shouldBe "m1"
            }
            Then("it treats tools without a stored toggle as enabled") {
                val repository = createRepository(
                    storedPreferences(toolEnabledJson = """{"calculate":false}"""),
                )

                repository.isToolEnabled("calculate") shouldBe false
                repository.isToolEnabled("unknown_tool") shouldBe true
            }
        }
    }

    Given("no persisted settings") {
        When("a setting is updated") {
            Then("the change surfaces back through the settings flow") {
                val repository = createRepository()

                repository.setThemeMode(ThemeMode.LIGHT)
                advanceUntilIdle()

                repository.value.themeMode shouldBe ThemeMode.LIGHT
            }
            Then("successive updates never overwrite each other") {
                val repository = createRepository()

                repository.setThemeMode(ThemeMode.DARK)
                repository.setWifiOnlyDownloads(true)
                advanceUntilIdle()

                repository.value.themeMode shouldBe ThemeMode.DARK
                repository.value.wifiOnlyDownloads shouldBe true
            }
        }
    }
})
