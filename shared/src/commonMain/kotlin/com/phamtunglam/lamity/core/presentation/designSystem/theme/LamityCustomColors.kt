package com.phamtunglam.lamity.core.presentation.designSystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colors Material 3 has no roles for (chat bubbles); read them via
 * [LamityTheme.colors] so screens never hard-code palette values.
 */
@Immutable
data class LamityCustomColors(
    val userBubble: Color,
    val onUserBubble: Color,
    val assistantBubble: Color,
    val onAssistantBubble: Color,
    val thoughtBubble: Color,
    val onThoughtBubble: Color,
    val toolBubble: Color,
    val onToolBubble: Color,
)

internal val LightCustomColors =
    LamityCustomColors(
        userBubble = LightPrimaryContainer,
        onUserBubble = LightOnPrimaryContainer,
        assistantBubble = LightSurfaceVariant,
        onAssistantBubble = LightOnSurfaceVariant,
        thoughtBubble = LightSecondaryContainer.copy(alpha = 0.6f),
        onThoughtBubble = LightOnSecondaryContainer,
        toolBubble = LightTertiaryContainer.copy(alpha = 0.55f),
        onToolBubble = LightOnTertiaryContainer,
    )

internal val DarkCustomColors =
    LamityCustomColors(
        userBubble = DarkPrimaryContainer,
        onUserBubble = DarkOnPrimaryContainer,
        assistantBubble = DarkSurfaceVariant,
        onAssistantBubble = DarkOnSurfaceVariant,
        thoughtBubble = DarkSecondaryContainer.copy(alpha = 0.6f),
        onThoughtBubble = DarkOnSecondaryContainer,
        toolBubble = DarkTertiaryContainer.copy(alpha = 0.55f),
        onToolBubble = DarkOnTertiaryContainer,
    )

internal val LocalLamityColors = staticCompositionLocalOf { LightCustomColors }
