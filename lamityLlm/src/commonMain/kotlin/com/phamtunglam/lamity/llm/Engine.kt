package com.phamtunglam.lamity.llm

import com.phamtunglam.lamity.llm.model.ConversationConfig
import com.phamtunglam.lamity.llm.model.EngineConfig
import com.phamtunglam.lamity.llm.native.EngineHandle
import com.phamtunglam.lamity.llm.native.LiteRtLmNativeRuntime
import com.phamtunglam.lamity.llm.native.createNativeRuntime
import com.phamtunglam.lamity.llm.tool.ToolManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A LiteRT-LM engine: load a model, then create conversations from it.
 */
class Engine(val engineConfig: EngineConfig) {
    private val runtime: LiteRtLmNativeRuntime = createNativeRuntime()
    private var handle: EngineHandle? = null

    val isInitialized: Boolean get() = handle != null

    /** Loads and initializes the native engine. Heavy (seconds); runs off the main thread. */
    suspend fun initialize() {
        if (handle != null) throw LiteRtLmException("Engine is already initialized")
        handle = withContext(Dispatchers.Default) { runtime.createEngine(engineConfig) }
    }

    /** Creates a new conversation from the initialized engine. */
    suspend fun createConversation(config: ConversationConfig = ConversationConfig()): Conversation {
        val engine = handle ?: throw LiteRtLmException("Engine is not initialized")
        val toolManager = ToolManager(config.tools)
        val conversation =
            withContext(Dispatchers.Default) {
                runtime.createConversation(
                    engine = engine,
                    systemMessage = config.systemMessage,
                    initialMessages = config.initialMessages,
                    samplerConfig = config.samplerConfig,
                    toolsJson = toolManager.toolsJsonDescription,
                )
            }
        return Conversation(runtime, conversation, toolManager)
    }

    /** Releases the native engine. */
    suspend fun dispose() {
        val engine = handle ?: return
        handle = null
        withContext(Dispatchers.Default) { runtime.deleteEngine(engine) }
    }
}
