package com.phamtunglam.lamity.llm

import com.phamtunglam.lamity.llm.model.InputData
import com.phamtunglam.lamity.llm.native.SessionHandle
import com.phamtunglam.lamity.llm.native.SessionNativeRuntime
import com.phamtunglam.lamity.llm.native.SessionStreamCallback
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
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
class Session internal constructor(
    private val runtime: SessionNativeRuntime,
    handle: SessionHandle,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private var handle: SessionHandle? = handle

    val isAlive: Boolean get() = handle != null

    private fun requireHandle(): SessionHandle = handle ?: throw LiteRtLmException("Session has been disposed")

    /** Runs the prefill step for [inputData]. */
    suspend fun runPrefill(inputData: List<InputData>) {
        if (inputData.isEmpty()) throw LiteRtLmException("runPrefill requires at least one input")
        withContext(dispatcher) { runtime.runSessionPrefill(requireHandle(), inputData) }
    }

    /** Runs the decode step and returns the generated text. */
    suspend fun runDecode(): String = withContext(dispatcher) { runtime.runSessionDecode(requireHandle()) }

    /** Generates content from [inputData] and returns the full text. */
    suspend fun generateContent(inputData: List<InputData>): String =
        withContext(dispatcher) { runtime.generateSessionContent(requireHandle(), inputData) }

    /** Generates content from [inputData] and streams the response as text chunks. */
    fun generateContentStream(inputData: List<InputData>): Flow<String> =
        channelFlow {
            val handle = requireHandle()
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
        val current = handle ?: return
        runtime.cancelSession(current)
    }

    /** Releases the native session. */
    fun dispose() {
        val current = handle ?: return
        handle = null
        runtime.deleteSession(current)
    }
}
