package com.phamtunglam.lamity.feature.theme.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.phamtunglam.lamity.feature.theme.domain.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [ThemeRepository] backed by the app-wide preferences [DataStore]. Reads observe the stored
 * preference reactively; writes go through [DataStore.edit], which is serialized.
 */
class ThemeRepositoryImpl(private val dataStore: DataStore<Preferences>) : ThemeRepository {
    override val theme: Flow<ThemeMode> =
        dataStore.data.map { preferences -> ThemeMode.fromStoredName(preferences[ThemeKey]) ?: ThemeMode.SYSTEM }

    override suspend fun setTheme(mode: ThemeMode) {
        dataStore.edit { preferences -> preferences[ThemeKey] = mode.name }
    }
}

private val ThemeKey = stringPreferencesKey("theme_mode")
