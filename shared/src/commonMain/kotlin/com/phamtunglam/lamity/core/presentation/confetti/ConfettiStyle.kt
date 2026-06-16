package com.phamtunglam.lamity.core.presentation.confetti

/** The visual style of a confetti celebration. */
enum class ConfettiStyle {
    /** A short, wide burst from the upper-middle of the screen. The default. */
    FESTIVE,

    /** Confetti falling from the top across the full width. */
    RAIN,

    /** A large, fast burst from the center of the screen. */
    EXPLOSION,
    ;

    companion object {
        /** Resolves a style from its (case-insensitive) [name], or null when unrecognised. */
        fun fromName(name: String?): ConfettiStyle? {
            val trimmed = name?.trim() ?: return null
            return entries.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
        }
    }
}
