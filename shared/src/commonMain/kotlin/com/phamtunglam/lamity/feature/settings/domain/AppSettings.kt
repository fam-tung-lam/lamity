package com.phamtunglam.lamity.feature.settings.domain

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    /** The model selected for chatting; chosen on the Models screen, restored when a chat opens. */
    val lastModelId: String? = null,
    /** Restrict model downloads to unmetered networks. */
    val wifiOnlyDownloads: Boolean = false,
)
