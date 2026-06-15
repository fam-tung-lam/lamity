package com.phamtunglam.lamity.llm.model

/**
 * The backend used by the LiteRT-LM engine.
 */
sealed class Backend(val name: String) {
    /** CPU backend. [threadCount] is honored on Android; the iOS C API ignores it. */
    class Cpu(val threadCount: Int? = null) : Backend("cpu")

    /** GPU backend. */
    class Gpu : Backend("gpu")
}
