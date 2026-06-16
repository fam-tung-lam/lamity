package com.phamtunglam.lamity.unitTests.feature.theme.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.phamtunglam.lamity.feature.theme.data.ThemeRepositoryImpl
import com.phamtunglam.lamity.feature.theme.domain.ThemeMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

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

private fun storedPreferences(theme: String? = null): Preferences =
    mutablePreferencesOf().apply {
        theme?.let { this[stringPreferencesKey("theme_mode")] = it }
    }

class ThemeRepositoryImplTest :
    BehaviorSpec({

        // The preferences DataStore is the single source of truth: the in-memory
        // fake persists writes and the repository observes them back.

        Given("a persisted theme") {
            When("the repository is read") {
                Then("it emits the stored theme") {
                    val repository = ThemeRepositoryImpl(FakePreferencesDataStore(storedPreferences(theme = "DARK")))

                    repository.theme.first() shouldBe ThemeMode.DARK
                }
            }
        }

        Given("no persisted theme") {
            When("the repository is read") {
                Then("it defaults to SYSTEM") {
                    val repository = ThemeRepositoryImpl(FakePreferencesDataStore(emptyPreferences()))

                    repository.theme.first() shouldBe ThemeMode.SYSTEM
                }
            }
            When("a theme is set") {
                Then("the change surfaces back through the theme flow") {
                    val repository = ThemeRepositoryImpl(FakePreferencesDataStore(emptyPreferences()))

                    repository.setTheme(ThemeMode.LIGHT)

                    repository.theme.first() shouldBe ThemeMode.LIGHT
                }
            }
        }
    })
