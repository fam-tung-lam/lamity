package com.phamtunglam.lamity.feature.chat.domain

import com.phamtunglam.lamity.llm.Conversation
import com.phamtunglam.lamity.llm.Engine
import com.phamtunglam.lamity.llm.model.ConversationConfig
import com.phamtunglam.lamity.llm.model.EngineConfig
import com.phamtunglam.lamity.llm.model.Message
import com.phamtunglam.lamity.logger.LamityLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

private const val TAG = "ModelRuntime"

/**
 * Developer-friendly facade over the lamityLlm [Engine]/[Conversation] API: owns the loaded engine,
 * serializes load/unload, tracks engine state, and exposes generation as a [Flow] of [GenEvent].
 * Consumers that want the full LiteRT-LM surface can use [Engine] directly.
 */
class ModelRuntime(private val dispatcher: CoroutineDispatcher = Dispatchers.Default) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val _engineState = MutableStateFlow<EngineState>(EngineState.Idle)
    val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

    private val mutex = Mutex()
    private var engine: Engine? = null
    private var engineKey: String? = null
    private val conversations = mutableMapOf<String, Conversation>()

    /**
     * Loads the engine for [config] unless an engine with the same [key] is already loaded. [key]
     * must change whenever a setting that requires an engine reload changes (model, backend, tokens).
     */
    @Suppress("TooGenericExceptionCaught")
    suspend fun ensureEngine(modelId: String, key: String, config: EngineConfig): Result<Unit> =
        mutex.withLock {
            if (engine != null && engineKey == key && _engineState.value is EngineState.Ready) {
                return Result.success(Unit)
            }
            disposeEngineLocked()
            LamityLogger.i(TAG) {
                "loading engine for $modelId (${config.backend.name}, ${config.maxNumTokens} tokens)"
            }
            _engineState.value = EngineState.Loading(modelId)
            return try {
                val created = Engine(config)
                created.initialize()
                engine = created
                engineKey = key
                _engineState.value = EngineState.Ready(modelId, key)
                Result.success(Unit)
            } catch (t: Throwable) {
                LamityLogger.e(TAG) { "engine load failed for $modelId: ${t.message}" }
                _engineState.value = EngineState.Error(modelId, t.message ?: t.toString())
                Result.failure(t)
            }
        }

    fun unloadEngine() {
        scope.launch { mutex.withLock { disposeEngineLocked() } }
    }

    private suspend fun disposeEngineLocked() {
        conversations.values.forEach { runCatching { it.dispose() } }
        conversations.clear()
        engine?.let { runCatching { it.dispose() } }
        engine = null
        engineKey = null
        _engineState.value = EngineState.Idle
    }

    @Suppress("TooGenericExceptionCaught")
    suspend fun createConversation(config: ConversationConfig): Result<String> {
        val current = engine ?: return Result.failure(IllegalStateException("Engine is not initialized"))
        return try {
            val conversation = current.createConversation(config)
            val handle = Random.nextLong().toULong().toString(radix = 16)
            conversations[handle] = conversation
            Result.success(handle)
        } catch (t: Throwable) {
            LamityLogger.e(TAG) { "createConversation failed: ${t.message}" }
            Result.failure(t)
        }
    }

    fun closeConversation(handle: String) {
        conversations.remove(handle)?.let { runCatching { it.dispose() } }
    }

    fun cancelGeneration(handle: String) {
        conversations[handle]?.let { runCatching { it.cancel() } }
    }

    /** Streams one generation; cancelling the collection aborts the native generation. */
    fun generate(handle: String, text: String): Flow<GenEvent> =
        flow {
            val conversation =
                conversations[handle]
                    ?: run {
                        emit(GenEvent.Error("Conversation is not active"))
                        return@flow
                    }
            conversation.sendMessageStream(Message.user(text)).collect { message ->
                if (message.text.isNotEmpty()) emit(GenEvent.Chunk(message.text))
                message.channels["thought"]?.takeIf { it.isNotEmpty() }?.let { emit(GenEvent.Thought(it)) }
            }
            emit(GenEvent.Done)
        }.catch { cause -> emit(GenEvent.Error(cause.message ?: cause.toString())) }
}
