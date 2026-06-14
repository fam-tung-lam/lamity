package com.phamtunglam.lamity.feature.localization.domain

/**
 * Supported concrete application locales.
 *
 * @property bcp47 BCP 47 language tag used by platform locale APIs.
 * @property displayName Endonym shown in the language picker (always in its own language).
 */
enum class AppLocale(val bcp47: String, val displayName: String) {
    /** English locale. */
    ENGLISH("en", "English"),

    /** Spanish locale. */
    SPANISH("es", "Español"),

    /** Vietnamese locale. */
    VIETNAMESE("vi", "Tiếng Việt"),
    ;

    companion object {
        /** Returns the stored locale, or null when [name] is invalid. */
        fun fromStoredName(name: String?): AppLocale? =
            name
                ?.takeIf(String::isNotEmpty)
                ?.let { storedName -> entries.firstOrNull { it.name == storedName } }

        /** Returns the supported locale matching [tag]'s primary language. */
        fun fromDeviceLocaleTag(tag: String?): AppLocale? =
            tag
                ?.primaryLanguage()
                ?.let { deviceLanguage ->
                    entries.firstOrNull { it.bcp47.primaryLanguage() == deviceLanguage }
                }
    }
}

private fun String.primaryLanguage(): String =
    trim()
        .substringBefore('-')
        .substringBefore('_')
        .lowercase()
