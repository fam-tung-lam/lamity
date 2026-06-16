package com.phamtunglam.lamity.core.presentation.designSystem.icons

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The Material "Tune" (sliders) glyph used for the chat settings entry point. Defined locally — with
 * the official 24dp path data — so the app keeps depending on `material-icons-core` only instead of
 * pulling in the whole `material-icons-extended` artifact for a single icon. `Icon` tints the black
 * fill with the current content color, exactly like a built-in icon.
 */
val Icons.Filled.Tune: ImageVector
    get() {
        cached?.let { return it }
        return ImageVector
            .Builder(
                name = "Filled.Tune",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(3.0f, 17.0f)
                    verticalLineToRelative(2.0f)
                    horizontalLineToRelative(6.0f)
                    verticalLineToRelative(-2.0f)
                    horizontalLineTo(3.0f)
                    close()
                    moveTo(3.0f, 5.0f)
                    verticalLineToRelative(2.0f)
                    horizontalLineToRelative(10.0f)
                    verticalLineTo(5.0f)
                    horizontalLineTo(3.0f)
                    close()
                    moveTo(13.0f, 21.0f)
                    verticalLineToRelative(-2.0f)
                    horizontalLineToRelative(8.0f)
                    verticalLineToRelative(-2.0f)
                    horizontalLineToRelative(-8.0f)
                    verticalLineToRelative(-2.0f)
                    horizontalLineToRelative(-2.0f)
                    verticalLineToRelative(6.0f)
                    horizontalLineToRelative(2.0f)
                    close()
                    moveTo(7.0f, 9.0f)
                    verticalLineToRelative(2.0f)
                    horizontalLineTo(3.0f)
                    verticalLineToRelative(2.0f)
                    horizontalLineToRelative(4.0f)
                    verticalLineToRelative(2.0f)
                    horizontalLineToRelative(2.0f)
                    verticalLineTo(9.0f)
                    horizontalLineTo(7.0f)
                    close()
                    moveTo(21.0f, 13.0f)
                    verticalLineToRelative(-2.0f)
                    horizontalLineTo(11.0f)
                    verticalLineToRelative(2.0f)
                    horizontalLineToRelative(10.0f)
                    close()
                    moveTo(15.0f, 9.0f)
                    horizontalLineToRelative(2.0f)
                    verticalLineTo(7.0f)
                    horizontalLineToRelative(4.0f)
                    verticalLineTo(5.0f)
                    horizontalLineToRelative(-4.0f)
                    verticalLineTo(3.0f)
                    horizontalLineToRelative(-2.0f)
                    verticalLineToRelative(6.0f)
                    close()
                }
            }.build()
            .also { cached = it }
    }

private var cached: ImageVector? = null
