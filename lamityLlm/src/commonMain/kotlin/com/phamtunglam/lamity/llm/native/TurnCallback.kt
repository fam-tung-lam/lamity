package com.phamtunglam.lamity.llm.native

import com.phamtunglam.lamity.llm.model.Message

/**
 * Callbacks for a single streamed model turn. The native runtime streams exactly one turn; the
 * common [com.phamtunglam.lamity.llm.Conversation] drives the tool-call loop across turns.
 */
internal interface TurnCallback {
    /** A delta chunk: a [Message] that may carry content, channels and/or tool calls. */
    fun onChunk(message: Message)

    /** The turn finished (the model stopped, possibly after requesting tools). */
    fun onComplete()

    /** The turn failed. */
    fun onError(message: String)
}
