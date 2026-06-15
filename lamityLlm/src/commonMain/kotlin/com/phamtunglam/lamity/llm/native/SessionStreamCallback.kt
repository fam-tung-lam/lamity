package com.phamtunglam.lamity.llm.native

/** Receives text chunks from a streaming [com.phamtunglam.lamity.llm.Session] generation. */
internal interface SessionStreamCallback {
    /** A piece of generated text. */
    fun onChunk(text: String)

    /** Generation finished successfully. */
    fun onComplete()

    /** Generation failed. */
    fun onError(message: String)
}
