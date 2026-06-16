package com.phamtunglam.lamity.llm

import com.phamtunglam.lamity.llm.model.ConversationConfig
import com.phamtunglam.lamity.llm.model.EngineConfig
import com.phamtunglam.lamity.llm.model.SessionConfig
import com.phamtunglam.lamity.llm.native.ConversationNativeRuntime
import com.phamtunglam.lamity.llm.native.EngineHandle
import com.phamtunglam.lamity.llm.native.EngineNativeRuntime
import com.phamtunglam.lamity.llm.native.SessionNativeRuntime
import com.phamtunglam.lamity.llm.native.createConversationNativeRuntime
import com.phamtunglam.lamity.llm.native.createEngineNativeRuntime
import com.phamtunglam.lamity.llm.native.createSessionNativeRuntime
import com.phamtunglam.lamity.llm.tool.ToolManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A LiteRT-LM engine: load a model, then create conversations or sessions from it.
 *
 * The public constructor wires the real native runtime. The collaborators are injected through the
 * primary [internal] constructor so tests can supply fakes; [dispatcher] is propagated to every
 * [Conversation]/[Session] this engine creates.
 */
class Engine internal constructor(
    val engineConfig: EngineConfig,
    private val runtime: EngineNativeRuntime,
    private val conversationRuntimeProvider: () -> ConversationNativeRuntime,
    private val sessionRuntimeProvider: () -> SessionNativeRuntime,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    constructor(engineConfig: EngineConfig) : this(
        engineConfig,
        createEngineNativeRuntime(),
        ::createConversationNativeRuntime,
        ::createSessionNativeRuntime,
        Dispatchers.Default,
    )

    private var handle: EngineHandle? = null

    val isInitialized: Boolean get() = handle != null

    /** Loads and initializes the native engine. Heavy (seconds); runs off the main thread. */
    suspend fun initialize() {
        if (handle != null) throw LiteRtLmException("Engine is already initialized")
        handle = withContext(dispatcher) { runtime.createEngine(engineConfig) }
    }

    /** Creates a new conversation from the initialized engine. */
    suspend fun createConversation(config: ConversationConfig = ConversationConfig()): Conversation {
        val engine = handle ?: throw LiteRtLmException("Engine is not initialized")
        val toolManager = ToolManager(config.tools)
        val conversation =
            withContext(dispatcher) {
                runtime.createConversation(
                    engine = engine,
                    systemMessage = config.systemMessage,
                    initialMessages = config.initialMessages,
                    samplerConfig = config.samplerConfig,
                    loraConfig = config.loraConfig,
                    extraContext = config.extraContext,
                    toolsJson = toolManager.toolsJsonDescription,
                )
            }
        return Conversation(
            conversationRuntimeProvider(),
            conversation,
            toolManager,
            config.automaticToolCalling,
            dispatcher,
        )
    }

    /** Creates a new lower-level session from the initialized engine. */
    suspend fun createSession(config: SessionConfig = SessionConfig()): Session {
        val engine = handle ?: throw LiteRtLmException("Engine is not initialized")
        val session =
            withContext(dispatcher) {
                runtime.createSession(engine, config.samplerConfig, config.loraConfig)
            }
        return Session(sessionRuntimeProvider(), session, dispatcher)
    }

    /** Releases the native engine. */
    suspend fun dispose() {
        val engine = handle ?: return
        handle = null
        withContext(dispatcher) { runtime.deleteEngine(engine) }
    }
}
