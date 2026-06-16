package com.phamtunglam.lamity.feature.theme.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phamtunglam.lamity.feature.theme.domain.ObserveThemeUseCase
import com.phamtunglam.lamity.feature.theme.domain.SetThemeUseCase
import com.phamtunglam.lamity.feature.theme.domain.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Theme selection state.
 *
 * @property current Selected theme.
 * @property options Available themes.
 */
data class ThemeUiState(
    val current: ThemeMode = ThemeMode.SYSTEM,
    val options: List<ThemeMode> = ThemeMode.entries.toList(),
)

/** Drives theme selection and persistence. */
class ThemeViewModel(observeTheme: ObserveThemeUseCase, private val setTheme: SetThemeUseCase) : ViewModel() {
    /** Emits the current theme selection. */
    val state: StateFlow<ThemeUiState> =
        observeTheme()
            .map { theme -> ThemeUiState(current = theme) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeUiState())

    /** Persists [mode]. */
    fun onThemeSelected(mode: ThemeMode) {
        viewModelScope.launch { setTheme(mode) }
    }
}
