package com.phamtunglam.lamity.feature.chat.domain

/** A single generation event surfaced to consumers. */
sealed interface GenEvent {
    data class Chunk(val text: String) : GenEvent

    data class Thought(val text: String) : GenEvent

    data object Done : GenEvent

    data class Error(val message: String) : GenEvent
}
