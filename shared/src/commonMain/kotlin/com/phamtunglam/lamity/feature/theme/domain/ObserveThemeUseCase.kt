package com.phamtunglam.lamity.feature.theme.domain

import com.phamtunglam.lamity.feature.theme.data.ThemeRepository
import kotlinx.coroutines.flow.Flow

/** Streams the selected app theme. */
class ObserveThemeUseCase(private val repository: ThemeRepository) {
    operator fun invoke(): Flow<ThemeMode> = repository.theme
}
