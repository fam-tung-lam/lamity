package com.phamtunglam.lamity.feature.theme.data

import com.phamtunglam.lamity.feature.theme.domain.ThemeMode
import kotlinx.coroutines.flow.Flow

/** Persists and observes the selected app theme. */
interface ThemeRepository {
    /** Emits the selected theme, defaulting to [ThemeMode.SYSTEM] when unset. */
    val theme: Flow<ThemeMode>

    /** Stores [mode]. */
    suspend fun setTheme(mode: ThemeMode)
}
