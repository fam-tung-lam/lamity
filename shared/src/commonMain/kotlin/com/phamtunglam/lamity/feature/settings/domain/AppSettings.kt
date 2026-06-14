package com.phamtunglam.lamity.feature.settings.domain

import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode { LIGHT, DARK, SYSTEM }

@Serializable
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Global tool toggles; a missing key means enabled. */
    val toolEnabled: Map<String, Boolean> = emptyMap(),
    val lastModelId: String? = null,
    val lastAgentId: String? = null,
    /** Restrict model downloads to unmetered networks. */
    val wifiOnlyDownloads: Boolean = false,
)
