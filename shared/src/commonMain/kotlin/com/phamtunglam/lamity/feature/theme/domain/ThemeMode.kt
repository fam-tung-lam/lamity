package com.phamtunglam.lamity.feature.theme.domain

/** Selectable application color theme. */
enum class ThemeMode {
    /** Always light. */
    LIGHT,

    /** Always dark. */
    DARK,

    /** Follows the device light/dark setting. */
    SYSTEM,
    ;

    companion object {
        /** Returns the stored theme, or null when [name] is missing or invalid. */
        fun fromStoredName(name: String?): ThemeMode? =
            name
                ?.takeIf(String::isNotEmpty)
                ?.let { storedName -> entries.firstOrNull { it.name == storedName } }
    }
}
