package com.phamtunglam.lamity.feature.localization.presentation

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

actual object LocalAppLocale {
    private var defaultLocale: Locale? = null

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        if (defaultLocale == null) {
            defaultLocale = Locale.getDefault()
        }
        val locale = value?.let(Locale::forLanguageTag) ?: defaultLocale ?: Locale.getDefault()
        Locale.setDefault(locale)
        val configuration =
            Configuration(LocalConfiguration.current).apply {
                setLocale(locale)
            }
        return LocalConfiguration provides configuration
    }
}
