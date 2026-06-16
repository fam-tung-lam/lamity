package com.phamtunglam.lamity.feature.settings.domain

import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode { LIGHT, DARK, SYSTEM }

@Serializable
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** The model selected for chatting; chosen on the Models screen, restored when a chat opens. */
    val lastModelId: String? = null,
    /** Restrict model downloads to unmetered networks. */
    val wifiOnlyDownloads: Boolean = false,
)
