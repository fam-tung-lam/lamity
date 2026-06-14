package com.phamtunglam.lamity.feature.localization.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.key
import com.phamtunglam.lamity.feature.localization.domain.AppLocale

/** Applies [locale] to Compose Multiplatform resources, recomposing on change. */
@Composable
fun AppLocaleEnvironment(locale: AppLocale?, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppLocale provides locale?.bcp47) {
        key(locale) {
            content()
        }
    }
}

/** Platform locale environment used by Compose resources. */
expect object LocalAppLocale {
    /** Creates a platform-specific locale provider value. */
    @Composable
    infix fun provides(value: String?): ProvidedValue<*>
}
