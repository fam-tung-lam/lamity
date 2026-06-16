package com.phamtunglam.lamity.core.presentation.confetti

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.vinceglb.confettikit.compose.ConfettiKit
import io.github.vinceglb.confettikit.core.Angle
import io.github.vinceglb.confettikit.core.Party
import io.github.vinceglb.confettikit.core.Position
import io.github.vinceglb.confettikit.core.Spread
import io.github.vinceglb.confettikit.core.emitter.Emitter
import kotlinx.coroutines.flow.SharedFlow
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** An active burst. The monotonically increasing [id] forces a fresh [ConfettiKit] per request. */
private data class ActiveBurst(val id: Int, val parties: List<Party>)

/**
 * A full-screen, non-interactive overlay that plays a confetti burst whenever [events] emits. Mount
 * it once at the app root, above the content. ConfettiKit draws into a Compose Canvas that does not
 * consume pointer input, so the overlay never blocks touches.
 */
@Composable
fun ConfettiOverlay(events: SharedFlow<ConfettiStyle>) {
    var burst by remember { mutableStateOf<ActiveBurst?>(null) }

    LaunchedEffect(events) {
        var nextId = 0
        events.collect { style -> burst = ActiveBurst(nextId++, style.toParties()) }
    }

    // ConfettiKit reads `parties` only once (in a LaunchedEffect(Unit)), so keying on the burst id
    // recreates it from scratch to restart the animation on every request.
    burst?.let { active ->
        key(active.id) {
            ConfettiKit(
                modifier = Modifier.fillMaxSize(),
                parties = active.parties,
                onParticleSystemEnded = { _, activeSystems ->
                    if (activeSystems == 0) burst = null
                },
            )
        }
    }
}

private fun ConfettiStyle.toParties(): List<Party> =
    when (this) {
        ConfettiStyle.FESTIVE -> listOf(festiveParty)
        ConfettiStyle.RAIN -> rainParties
        ConfettiStyle.EXPLOSION -> listOf(explosionParty)
    }

private val confettiColors = listOf(0xFCE18A, 0xFF726D, 0xF4306D, 0xB48DEF, 0x6FE7DD, 0xFFD93D)

private val festiveParty =
    Party(
        speed = 0f,
        maxSpeed = 30f,
        damping = 0.9f,
        spread = Spread.ROUND,
        colors = confettiColors,
        emitter = Emitter(100.milliseconds).max(100),
        position = Position.Relative(0.5, 0.3),
    )

private val explosionParty =
    Party(
        speed = 0f,
        maxSpeed = 50f,
        damping = 0.9f,
        spread = Spread.ROUND,
        colors = confettiColors,
        emitter = Emitter(200.milliseconds).max(220),
        position = Position.Relative(0.5, 0.5),
    )

private val rainParties =
    listOf(
        Party(
            speed = 0f,
            maxSpeed = 15f,
            damping = 0.9f,
            angle = Angle.BOTTOM,
            spread = Spread.ROUND,
            colors = confettiColors,
            emitter = Emitter(2.seconds).perSecond(80),
            position = Position.Relative(0.0, 0.0).between(Position.Relative(1.0, 0.0)),
        ),
    )
