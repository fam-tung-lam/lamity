package com.phamtunglam.lamity.llm

import com.phamtunglam.lamity.llm.model.InputData
import com.phamtunglam.lamity.llm.native.SessionHandle
import com.phamtunglam.lamity.llm.native.SessionNativeRuntime
import com.phamtunglam.lamity.llm.native.SessionStreamCallback
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext

/**
 * A lower-level LiteRT-LM session exposing explicit prefill/decode and content generation,
 * created via [Engine.createSession].
 */
class Session internal constructor(private val runtime: SessionNativeRuntime, private val handle: SessionHandle) {
    private var alive = true
    val isAlive: Boolean get() = alive

    /** Runs the prefill step for [inputData]. */
    suspend fun runPrefill(inputData: List<InputData>) {
        if (inputData.isEmpty()) throw LiteRtLmException("runPrefill requires at least one input")
        withContext(Dispatchers.Default) { runtime.runSessionPrefill(handle, inputData) }
    }

    /** Runs the decode step and returns the generated text. */
    suspend fun runDecode(): String = withContext(Dispatchers.Default) { runtime.runSessionDecode(handle) }

    /** Generates content from [inputData] and returns the full text. */
    suspend fun generateContent(inputData: List<InputData>): String =
        withContext(Dispatchers.Default) { runtime.generateSessionContent(handle, inputData) }

    /** Generates content from [inputData] and streams the response as text chunks. */
    fun generateContentStream(inputData: List<InputData>): Flow<String> =
        channelFlow {
            val producer = this
            val done = CompletableDeferred<Unit>()
            runtime.generateSessionContentStream(
                handle,
                inputData,
                object : SessionStreamCallback {
                    override fun onChunk(text: String) {
                        producer.trySend(text)
                    }

                    override fun onComplete() {
                        done.complete(Unit)
                    }

                    override fun onError(message: String) {
                        done.completeExceptionally(LiteRtLmException(message))
                    }
                },
            )
            done.await()
        }.buffer(Channel.UNLIMITED)

    /** Cancels any ongoing session processing. */
    fun cancelProcess() {
        runtime.cancelSession(handle)
    }

    /** Releases the native session. */
    fun dispose() {
        if (!alive) return
        alive = false
        runtime.deleteSession(handle)
    }
}
