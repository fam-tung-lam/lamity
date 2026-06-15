package com.phamtunglam.lamity.llm

/** Error thrown by the LiteRT-LM API. */
class LiteRtLmException(message: String, cause: Throwable? = null) : Exception(message, cause)
