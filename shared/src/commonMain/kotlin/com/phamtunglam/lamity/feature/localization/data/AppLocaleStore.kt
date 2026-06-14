package com.phamtunglam.lamity.feature.localization.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.phamtunglam.lamity.feature.localization.domain.AppLocale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Persists the selected app locale. */
interface AppLocaleStore {
    /** Emits the selected locale, or null when the app follows the device locale. */
    val locale: Flow<AppLocale?>

    /** Stores [locale], or clears the override when [locale] is null. */
    suspend fun setLocale(locale: AppLocale?)
}

/** [AppLocaleStore] backed by a preferences [DataStore]. */
class DataStoreAppLocaleStore(private val dataStore: DataStore<Preferences>) : AppLocaleStore {
    override val locale: Flow<AppLocale?> =
        dataStore.data.map { preferences -> AppLocale.fromStoredName(preferences[AppLocaleKey]) }

    override suspend fun setLocale(locale: AppLocale?) {
        dataStore.edit { preferences ->
            if (locale == null) {
                preferences.remove(AppLocaleKey)
            } else {
                preferences[AppLocaleKey] = locale.name
            }
        }
    }
}

private val AppLocaleKey = stringPreferencesKey("app_locale")
