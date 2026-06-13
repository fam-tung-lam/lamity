package com.phamtunglam.lamity.llm

/**
 * Contract between shared code and the platform LiteRT-LM runtime.
 *
 * Android implements it in this module's androidMain with the LiteRT-LM
 * Kotlin API ([AndroidLlmBridge]). iOS implements it in Swift
 * (iosApp/LlmBridge.swift) with the LiteRT-LM Swift API, so every method is
 * callback-based — Swift classes cannot implement Kotlin suspend members.
 */
interface NativeLlmBridge {
    /** Set by shared code; platform runtimes route model tool calls through it. */
    var toolExecutor: ToolExecutor?

    fun initializeEngine(setup: EngineSetup, callback: EngineCallback)
    fun closeEngine()

    fun createConversation(setup: ConversationSetup, callback: ConversationCallback)
    fun closeConversation(handle: String)

    fun sendMessage(handle: String, text: String, listener: GenerationListener)
    fun cancelGeneration(handle: String)
}

/** Executes a tool synchronously and returns the result JSON. */
interface ToolExecutor {
    fun executeTool(toolId: String, paramsJson: String): String
}

class EngineSetup(
    val modelPath: String,
    /** "cpu" or "gpu". */
    val backend: String,
    val maxTokens: Int,
    val cacheDir: String,
)

class ConversationSetup(
    val systemPrompt: String?,
    /** JSON array: [{"role":"user"|"model","text":"..."}] replayed when resuming. */
    val historyJson: String,
    /** Enabled tool ids; iOS maps these to its Swift tool structs. */
    val toolIds: List<String>,
    /** JSON array of {name,description,parameters} specs; Android builds OpenApiTools from it. */
    val toolSpecsJson: String,
    val topK: Int,
    val topP: Double,
    val temperature: Double,
)

interface EngineCallback {
    fun onEngineReady()
    fun onEngineError(message: String)
}

interface ConversationCallback {
    fun onConversationReady(handle: String)
    fun onConversationError(message: String)
}

interface GenerationListener {
    fun onChunk(text: String)
    /** Reasoning-channel delta (DeepSeek-R1 style); may never be called. */
    fun onThought(text: String)
    fun onGenerationDone()
    fun onGenerationError(message: String)
}
