package com.phamtunglam.lamity.feature.localization.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.preferredLanguages

actual object LocalAppLocale {
    private const val APPLE_LANGUAGES_KEY = "AppleLanguages"
    private val DEFAULT_LOCALE = NSLocale.preferredLanguages.firstOrNull() as? String ?: "en"

    @Suppress("ktlint:standard:property-naming")
    private val LocalLocale = staticCompositionLocalOf { DEFAULT_LOCALE }

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val locale = value ?: DEFAULT_LOCALE
        if (value == null) {
            NSUserDefaults.standardUserDefaults.removeObjectForKey(APPLE_LANGUAGES_KEY)
        } else {
            NSUserDefaults.standardUserDefaults.setObject(arrayListOf(locale), APPLE_LANGUAGES_KEY)
        }
        return LocalLocale provides locale
    }
}
