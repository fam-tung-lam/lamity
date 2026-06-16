package com.phamtunglam.lamity.feature.theme.domain

import com.phamtunglam.lamity.feature.theme.data.ThemeRepository

/** Persists [mode] as the selected app theme. */
class SetThemeUseCase(private val repository: ThemeRepository) {
    suspend operator fun invoke(mode: ThemeMode) = repository.setTheme(mode)
}
