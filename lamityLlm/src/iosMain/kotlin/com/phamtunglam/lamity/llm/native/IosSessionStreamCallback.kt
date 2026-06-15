@file:OptIn(ExperimentalForeignApi::class)

package com.phamtunglam.lamity.llm.native

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.toKString

/**
 * Static C callback for `litert_lm_session_generate_content_stream` (runs on a native background
 * thread per chunk). Session stream chunks are raw text, not message JSON.
 */
internal fun iosSessionStreamCallback(
    userData: COpaquePointer?,
    chunk: CPointer<ByteVar>?,
    isFinal: Boolean,
    errorMessage: CPointer<ByteVar>?,
) {
    val ref = userData?.asStableRef<IosSessionContext>() ?: return
    val callback = ref.get().callback
    if (errorMessage != null) {
        callback.onError(errorMessage.toKString())
        ref.dispose()
        return
    }
    if (chunk != null) callback.onChunk(chunk.toKString())
    if (isFinal) {
        callback.onComplete()
        ref.dispose()
    }
}
