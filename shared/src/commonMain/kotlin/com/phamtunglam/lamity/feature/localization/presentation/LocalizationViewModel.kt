package com.phamtunglam.lamity.feature.localization.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phamtunglam.lamity.feature.localization.data.AppLocaleRepository
import com.phamtunglam.lamity.feature.localization.data.AppLocaleStore
import com.phamtunglam.lamity.feature.localization.domain.AppLocale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Locale selection state.
 *
 * @property current Selected locale, or null when following the device locale.
 * @property resolved Concrete locale currently applied.
 * @property options Available locale overrides.
 */
data class LocalizationUiState(
    val current: AppLocale? = null,
    val resolved: AppLocale = AppLocale.ENGLISH,
    val options: List<AppLocale> = AppLocale.entries.toList(),
)

/** Drives locale selection and persistence. */
class LocalizationViewModel(private val store: AppLocaleStore, repository: AppLocaleRepository) : ViewModel() {
    /** Emits the current override and resolved locale. */
    val state: StateFlow<LocalizationUiState> =
        combine(store.locale, repository.locale) { selected, resolved ->
            LocalizationUiState(current = selected, resolved = resolved)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocalizationUiState())

    /** Persists [locale], or clears the override when [locale] is null. */
    fun onLocaleSelected(locale: AppLocale?) {
        viewModelScope.launch { store.setLocale(locale) }
    }
}
