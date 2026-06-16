package com.phamtunglam.lamity.core.presentation.confetti

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * App-wide hub for one-shot confetti requests. Anything (e.g. the show_confetti tool) calls
 * [celebrate]; the [ConfettiOverlay] mounted at the app root collects [events] and plays each burst.
 *
 * A buffered, drop-oldest [MutableSharedFlow] keeps [celebrate] non-blocking even if a burst is
 * requested while nothing is collecting.
 */
class ConfettiController {
    private val mutableEvents =
        MutableSharedFlow<ConfettiStyle>(
            extraBufferCapacity = 4,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    /** Each requested burst. Hot and shared, with no replay (bursts are transient). */
    val events: SharedFlow<ConfettiStyle> = mutableEvents.asSharedFlow()

    /** Requests a confetti burst of the given [style]. */
    fun celebrate(style: ConfettiStyle = ConfettiStyle.FESTIVE) {
        mutableEvents.tryEmit(style)
    }
}
