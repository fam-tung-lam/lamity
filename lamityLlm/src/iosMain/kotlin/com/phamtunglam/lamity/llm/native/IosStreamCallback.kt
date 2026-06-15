@file:OptIn(ExperimentalForeignApi::class)

package com.phamtunglam.lamity.llm.native

import com.phamtunglam.lamity.llm.serialization.messageFromJsonString
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.toKString

/** `LiteRtLmStreamCallback` implementation — runs on a native background thread per chunk. */
internal fun iosStreamCallback(
    userData: COpaquePointer?,
    chunk: CPointer<ByteVar>?,
    isFinal: Boolean,
    errorMessage: CPointer<ByteVar>?,
) {
    val ref = userData?.asStableRef<IosTurnContext>() ?: return
    val callback = ref.get().callback
    if (errorMessage != null) {
        callback.onError(errorMessage.toKString())
        ref.dispose()
        return
    }
    if (chunk != null) {
        val message = runCatching { messageFromJsonString(chunk.toKString()) }.getOrNull()
        if (message != null) callback.onChunk(message)
    }
    if (isFinal) {
        callback.onComplete()
        ref.dispose()
    }
}
