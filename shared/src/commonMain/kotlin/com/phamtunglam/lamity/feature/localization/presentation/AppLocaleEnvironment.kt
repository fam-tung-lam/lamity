package com.phamtunglam.lamity.feature.localization.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import com.phamtunglam.lamity.feature.localization.domain.AppLocale

/**
 * Applies [locale] to Compose Multiplatform resources, recomposing on change.
 *
 * The locale flows through [LocalAppLocale] so resource readers recompose in place; the subtree is
 * intentionally not wrapped in `key(locale)`, which would tear it down and reset retained state such
 * as the navigation back stack on every locale change.
 */
@Composable
fun AppLocaleEnvironment(locale: AppLocale?, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppLocale provides locale?.bcp47) {
        content()
    }
}

/** Platform locale environment used by Compose resources. */
expect object LocalAppLocale {
    /** Creates a platform-specific locale provider value. */
    @Composable
    infix fun provides(value: String?): ProvidedValue<*>
}
