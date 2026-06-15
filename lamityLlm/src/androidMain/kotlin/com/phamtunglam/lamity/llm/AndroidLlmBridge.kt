package com.phamtunglam.lamity.llm

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.OpenApiTool
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ToolProvider
import com.google.ai.edge.litertlm.tool
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** LiteRT-LM Kotlin runtime behind the shared [NativeLlmBridge] contract. */
class AndroidLlmBridge : NativeLlmBridge {
    override var toolExecutor: ToolExecutor? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var engine: Engine? = null
    private val conversations = ConcurrentHashMap<String, Conversation>()
    private val activeJobs = ConcurrentHashMap<String, Job>()

    // Native LiteRT-LM init can surface arbitrary Throwables (JNI/native errors);
    // all are funneled to the engine error callback.
    @Suppress("TooGenericExceptionCaught")
    override fun initializeEngine(setup: EngineSetup, callback: EngineCallback) {
        scope.launch {
            try {
                closeEngineInternal()
                val backend =
                    if (setup.backend.equals("gpu", ignoreCase = true)) {
                        Backend.GPU()
                    } else {
                        Backend.CPU()
                    }
                val config =
                    EngineConfig(
                        modelPath = setup.modelPath,
                        backend = backend,
                        maxNumTokens = setup.maxTokens,
                        cacheDir = setup.cacheDir,
                    )
                val created = Engine(config)
                created.initialize()
                engine = created
                callback.onEngineReady()
            } catch (t: Throwable) {
                callback.onEngineError(t.message ?: t.toString())
            }
        }
    }

    override fun closeEngine() {
        scope.launch { closeEngineInternal() }
    }

    private fun closeEngineInternal() {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        conversations.values.forEach { runCatching { it.close() } }
        conversations.clear()
        engine?.let { runCatching { it.close() } }
        engine = null
    }

    // Native conversation setup can surface arbitrary Throwables; funnel them to the callback.
    @Suppress("TooGenericExceptionCaught")
    override fun createConversation(setup: ConversationSetup, callback: ConversationCallback) {
        scope.launch {
            try {
                val current = engine ?: error("Engine is not initialized")
                val config =
                    ConversationConfig(
                        systemInstruction =
                            setup.systemPrompt
                                ?.takeIf { it.isNotBlank() }
                                ?.let { Contents.of(Content.Text(it)) },
                        initialMessages = parseHistory(setup.historyJson),
                        tools = buildTools(setup.toolSpecsJson),
                        samplerConfig =
                            SamplerConfig(
                                topK = setup.topK,
                                topP = setup.topP,
                                temperature = setup.temperature,
                            ),
                    )
                val conversation = current.createConversation(config)
                val handle = UUID.randomUUID().toString()
                conversations[handle] = conversation
                callback.onConversationReady(handle)
            } catch (t: Throwable) {
                callback.onConversationError(t.message ?: t.toString())
            }
        }
    }

    override fun closeConversation(handle: String) {
        activeJobs.remove(handle)?.cancel()
        conversations.remove(handle)?.let { runCatching { it.close() } }
    }

    // Cancellation ends generation normally; any other native Throwable becomes a generation error.
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override fun sendMessage(handle: String, text: String, listener: GenerationListener) {
        val conversation = conversations[handle]
        if (conversation == null) {
            listener.onGenerationError("Conversation is not active")
            return
        }
        val job =
            scope.launch {
                try {
                    conversation.sendMessageAsync(text).collect { message: Message ->
                        val chunk = message.toString()
                        if (chunk.isNotEmpty()) listener.onChunk(chunk)
                        message.channels["thought"]?.takeIf { it.isNotEmpty() }?.let {
                            listener.onThought(it)
                        }
                    }
                    listener.onGenerationDone()
                } catch (c: CancellationException) {
                    listener.onGenerationDone()
                } catch (t: Throwable) {
                    listener.onGenerationError(t.message ?: t.toString())
                } finally {
                    activeJobs.remove(handle)
                }
            }
        activeJobs[handle] = job
    }

    override fun cancelGeneration(handle: String) {
        val job = activeJobs.remove(handle) ?: return
        runCatching { conversations[handle]?.cancelProcess() }
        job.cancel()
    }

    // -------------------------------------------------------------- helpers

    private fun parseHistory(historyJson: String): List<Message> =
        runCatching {
            Json.parseToJsonElement(historyJson).jsonArray.mapNotNull { element ->
                val obj = element.jsonObject
                val role = obj["role"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val text = obj["text"]?.jsonPrimitive?.content ?: return@mapNotNull null
                if (role == "user") Message.user(text) else Message.model(text)
            }
        }.getOrDefault(emptyList())

    private fun buildTools(toolSpecsJson: String): List<ToolProvider> =
        runCatching {
            Json.parseToJsonElement(toolSpecsJson).jsonArray.mapNotNull { element ->
                val obj = element.jsonObject
                val name = obj["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
                tool(
                    JsonSpecTool(name, element.toString()) { toolId, params ->
                        toolExecutor?.executeTool(toolId, params)
                            ?: """{"error":"tool executor not attached"}"""
                    },
                )
            }
        }.getOrDefault(emptyList())

    /** OpenApiTool whose spec comes from shared code and whose execution is delegated back to it. */
    private class JsonSpecTool(
        private val name: String,
        private val specJson: String,
        private val onExecute: (toolId: String, paramsJson: String) -> String,
    ) : OpenApiTool {
        override fun getToolDescriptionJsonString(): String = specJson

        override fun execute(paramsJsonString: String): String = onExecute(name, paramsJsonString)
    }
}
