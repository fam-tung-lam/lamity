package com.phamtunglam.lamity.llm

import co.touchlab.kermit.Logger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

sealed interface EngineState {
    data object Idle : EngineState

    data class Loading(val modelId: String) : EngineState

    data class Ready(val modelId: String, val engineKey: String) : EngineState

    data class Error(val modelId: String, val message: String) : EngineState
}

sealed interface GenEvent {
    data class Chunk(val text: String) : GenEvent

    data class Thought(val text: String) : GenEvent

    data object Done : GenEvent

    data class Error(val message: String) : GenEvent
}

/**
 * Developer-friendly coroutine/Flow facade over [NativeLlmBridge]: owns the
 * loaded engine, serializes load/unload, and wraps the callback contract into
 * suspend functions and cold [Flow]s.
 */
class ModelRuntime(private val bridge: NativeLlmBridge) {
    private val log = Logger.withTag("ModelRuntime")

    private val _engineState = MutableStateFlow<EngineState>(EngineState.Idle)
    val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

    private val mutex = Mutex()

    /**
     * Loads the engine for [setup] unless an engine with the same [engineKey]
     * is already loaded. [engineKey] must change whenever a setting that
     * requires an engine reload changes (model file, backend, max tokens).
     */
    suspend fun ensureEngine(modelId: String, engineKey: String, setup: EngineSetup): Result<Unit> =
        mutex.withLock {
            val current = _engineState.value
            if (current is EngineState.Ready && current.engineKey == engineKey) {
                return Result.success(Unit)
            }

            log.i { "loading engine for $modelId (${setup.backend}, ${setup.maxTokens} tokens)" }
            _engineState.value = EngineState.Loading(modelId)
            return suspendCancellableCoroutine { cont ->
                bridge.initializeEngine(
                    setup,
                    object : EngineCallback {
                        override fun onEngineReady() {
                            _engineState.value = EngineState.Ready(modelId, engineKey)
                            if (cont.isActive) cont.resume(Result.success(Unit))
                        }

                        override fun onEngineError(message: String) {
                            log.e { "engine load failed for $modelId: $message" }
                            _engineState.value = EngineState.Error(modelId, message)
                            if (cont.isActive) cont.resume(Result.failure(IllegalStateException(message)))
                        }
                    },
                )
            }
        }

    fun unloadEngine() {
        bridge.closeEngine()
        _engineState.value = EngineState.Idle
    }

    suspend fun createConversation(setup: ConversationSetup): Result<String> =
        suspendCancellableCoroutine { cont ->
            bridge.createConversation(
                setup,
                object : ConversationCallback {
                    override fun onConversationReady(handle: String) {
                        if (cont.isActive) cont.resume(Result.success(handle))
                    }

                    override fun onConversationError(message: String) {
                        log.e { "createConversation failed: $message" }
                        if (cont.isActive) cont.resume(Result.failure(IllegalStateException(message)))
                    }
                },
            )
        }

    fun closeConversation(handle: String) = bridge.closeConversation(handle)

    /** Streams one generation; cancelling the collection aborts the native generation. */
    fun generate(handle: String, text: String): Flow<GenEvent> =
        callbackFlow {
            bridge.sendMessage(
                handle,
                text,
                object : GenerationListener {
                    override fun onChunk(text: String) {
                        trySend(GenEvent.Chunk(text))
                    }

                    override fun onThought(text: String) {
                        trySend(GenEvent.Thought(text))
                    }

                    override fun onGenerationDone() {
                        trySend(GenEvent.Done)
                        close()
                    }

                    override fun onGenerationError(message: String) {
                        trySend(GenEvent.Error(message))
                        close()
                    }
                },
            )
            awaitClose { bridge.cancelGeneration(handle) }
        }.buffer(Channel.UNLIMITED)
}
