package com.phamtunglam.lamity.feature.chat.domain

import co.touchlab.kermit.Logger
import com.phamtunglam.lamity.core.domain.platform.AppDirs
import com.phamtunglam.lamity.core.domain.platform.epochMillis
import com.phamtunglam.lamity.core.domain.platform.newId
import com.phamtunglam.lamity.feature.agents.data.AgentsRepository
import com.phamtunglam.lamity.feature.agents.domain.Agent
import com.phamtunglam.lamity.feature.chat.data.ConversationsRepository
import com.phamtunglam.lamity.feature.models.data.ModelDownloadManager
import com.phamtunglam.lamity.feature.models.data.ModelsRepository
import com.phamtunglam.lamity.feature.models.domain.LlmBackend
import com.phamtunglam.lamity.feature.models.domain.LlmModel
import com.phamtunglam.lamity.feature.settings.data.SettingsRepository
import com.phamtunglam.lamity.feature.skills.data.SkillsRepository
import com.phamtunglam.lamity.feature.skills.domain.Skill
import com.phamtunglam.lamity.feature.tools.domain.AppTool
import com.phamtunglam.lamity.feature.tools.domain.LoadSkillTool
import com.phamtunglam.lamity.llm.model.Backend
import com.phamtunglam.lamity.llm.model.ConversationConfig
import com.phamtunglam.lamity.llm.model.EngineConfig
import com.phamtunglam.lamity.llm.model.Message
import com.phamtunglam.lamity.llm.model.SamplerConfig
import com.phamtunglam.lamity.llm.tool.Tool
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class ChatSessionState(
    val conversationId: String? = null,
    val agentId: String? = null,
    val modelId: String? = null,
    /** Per-chat tool ids when running without an agent; null = all enabled tools. */
    val customToolIds: List<String>? = null,
    /** Per-chat skill ids when running without an agent; null = none. */
    val customSkillIds: List<String>? = null,
    /** Per-chat system prompt when running without an agent. */
    val customSystemPrompt: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val streamingText: String = "",
    val streamingThought: String = "",
    val isGenerating: Boolean = false,
    val engine: EngineState = EngineState.Idle,
    val error: ChatError? = null,
    val notice: ChatNotice? = null,
)

/**
 * Orchestrates chats: sessions, streaming, tool events, history persistence.
 * Lives for the whole process so chat state survives navigation; ViewModels
 * are thin observers of [state].
 */
@Suppress("TooManyFunctions") // Cohesive chat orchestrator; splitting would scatter related logic.
class ChatSessionManager(
    private val scope: CoroutineScope,
    private val runtime: ModelRuntime,
    private val conversations: ConversationsRepository,
    private val agents: AgentsRepository,
    private val skills: SkillsRepository,
    private val models: ModelsRepository,
    private val settings: SettingsRepository,
    /** User-selectable built-in tools (everything except the per-session load_skill). */
    private val selectableTools: List<AppTool>,
    private val downloads: ModelDownloadManager,
    private val dirs: AppDirs,
) {
    private val log = Logger.withTag("ChatSessionManager")

    private val _state = MutableStateFlow(ChatSessionState())
    val state: StateFlow<ChatSessionState> = _state.asStateFlow()

    /** Tool invocations recorded during generation; persisted and shown as TOOL messages. */
    private val toolMessages = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 64)

    private var sessionHandle: String? = null
    private var sessionSignature: String? = null
    private var generateJob: Job? = null

    /** Serializes native session lifecycle so prepare and generate never race on [sessionHandle]. */
    private val sessionMutex = Mutex()
    private var prepareJob: Job? = null
    private var preparePending = false

    init {
        scope.launch {
            settings.awaitLoaded()
            models.awaitLoaded()
            agents.awaitLoaded()
            val s = settings.value
            _state.update {
                it.copy(
                    agentId = s.lastAgentId.takeIf { id -> agents.byId(id) != null },
                    modelId = (
                        s.lastModelId.takeIf { id -> models.byId(id) != null }
                            ?: models.models.value
                                .firstOrNull()
                                ?.id
                    ),
                )
            }
            // Warm the engine for the restored selection once everything has loaded.
            prepareSession()
        }
        scope.launch {
            runtime.engineState.collect { es -> _state.update { it.copy(engine = es) } }
        }
        scope.launch {
            toolMessages.collect { message ->
                conversations.appendMessage(message)
                _state.update { it.copy(messages = it.messages + message) }
            }
        }
        // Re-warm when a config change affects the engine or conversation: the active model's saved
        // config, or the set of globally enabled tools.
        scope.launch {
            models.models
                .drop(1)
                .collect { prepareSession() }
        }
        scope.launch {
            settings.settings
                .map { it.toolEnabled }
                .distinctUntilChanged()
                .drop(1)
                .collect { prepareSession() }
        }
    }

    // ----------------------------------------------------------- selection

    fun selectModel(modelId: String) {
        if (modelId == _state.value.modelId) return
        stopGeneration()
        _state.update { it.copy(modelId = modelId, error = null) }
        persistSelection()
        prepareSession()
    }

    fun selectAgent(agentId: String?) {
        if (agentId == _state.value.agentId) return
        stopGeneration()
        _state.update { it.copy(agentId = agentId, error = null) }
        persistSelection()
        prepareSession()
    }

    fun setCustomSystemPrompt(prompt: String) {
        _state.update { it.copy(customSystemPrompt = prompt.ifBlank { null }) }
        prepareSession()
    }

    fun toggleCustomTool(toolId: String) {
        _state.update {
            val current = it.customToolIds ?: selectableTools.map { tool -> tool.id }
            it.copy(customToolIds = if (toolId in current) current - toolId else current + toolId)
        }
        prepareSession()
    }

    fun toggleCustomSkill(skillId: String) {
        _state.update {
            val current = it.customSkillIds ?: emptyList()
            it.copy(customSkillIds = if (skillId in current) current - skillId else current + skillId)
        }
        prepareSession()
    }

    private fun persistSelection() {
        val s = _state.value
        scope.launch { settings.setLastSelection(s.modelId, s.agentId) }
    }

    // ----------------------------------------------------------- preparation

    /**
     * Eagerly loads the engine and (re)builds the native conversation for the current selection so
     * the first message streams without an upfront load. Idempotent — [ModelRuntime.ensureEngine]
     * and [ensureSession] dedupe by key/signature — and a no-op while generating. Coalesces rapid
     * calls and re-runs once if the selection changed mid-load.
     */
    @Suppress("TooGenericExceptionCaught")
    fun prepareSession() {
        if (prepareJob?.isActive == true) {
            preparePending = true
            return
        }
        prepareJob =
            scope.launch {
                do {
                    preparePending = false
                    try {
                        doPrepare()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        log.e(e) { "session prepare failed" }
                    }
                } while (preparePending)
            }
    }

    private suspend fun doPrepare() {
        if (_state.value.isGenerating) return
        val agent = agents.byId(_state.value.agentId)
        val model = resolveModel(agent) ?: return
        if (!downloads.isDownloaded(model)) return
        val effectiveModel = loadEngineWithFallback(model) ?: return
        ensureSession(effectiveModel, agent, _state.value.conversationId)
    }

    /** The model to run for [agent] (its own model + config override) or the chat-selected model. */
    private fun resolveModel(agent: Agent?): LlmModel? {
        if (agent?.modelId != null) {
            val base = models.byId(agent.modelId) ?: return null
            return agent.modelConfig?.let { base.copy(config = it) } ?: base
        }
        return models.byId(_state.value.modelId)
    }

    // ----------------------------------------------------------- lifecycle

    fun newChat() {
        stopGeneration()
        _state.update {
            it.copy(
                conversationId = null,
                messages = emptyList(),
                streamingText = "",
                streamingThought = "",
                error = null,
            )
        }
        // The native conversation is rebuilt by prepare/send: the empty history + null id give a
        // different session signature, so ensureSession discards the old one.
        prepareSession()
    }

    fun openConversation(conversationId: String) {
        val conversation = conversations.byId(conversationId) ?: return
        stopGeneration()
        scope.launch {
            val messages = conversations.loadMessages(conversationId)
            _state.update {
                it.copy(
                    conversationId = conversation.id,
                    agentId = conversation.agentId.takeIf { id -> agents.byId(id) != null },
                    modelId = models.byId(conversation.modelId)?.id ?: it.modelId,
                    customToolIds = conversation.customToolIds,
                    customSkillIds = conversation.customSkillIds,
                    customSystemPrompt = conversation.customSystemPrompt,
                    messages = messages,
                    streamingText = "",
                    streamingThought = "",
                    error = null,
                )
            }
            persistSelection()
            prepareSession()
        }
    }

    fun onConversationDeleted(conversationId: String) {
        if (_state.value.conversationId == conversationId) newChat()
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    fun dismissNotice() = _state.update { it.copy(notice = null) }

    // ----------------------------------------------------------- generation

    // Surfaces any generation failure (including arbitrary native LLM errors) to the UI; cancellation is rethrown.
    @Suppress("TooGenericExceptionCaught")
    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _state.value.isGenerating) return
        val agent = agents.byId(_state.value.agentId)
        val model = resolveModel(agent)
        if (model == null) {
            _state.update { it.copy(error = ChatError.Raw("Select a model first")) }
            return
        }
        if (!downloads.isDownloaded(model)) {
            _state.update { it.copy(error = ChatError.Raw("Model is not downloaded yet")) }
            return
        }

        generateJob =
            scope.launch {
                _state.update {
                    it.copy(
                        isGenerating = true,
                        error = null,
                        notice = null,
                        streamingText = "",
                        streamingThought = "",
                    )
                }
                try {
                    runGeneration(agent, model, trimmed)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    log.e(e) { "generation failed" }
                    _state.update { it.copy(error = ChatError.Raw(e.message ?: "Generation failed")) }
                } finally {
                    _state.update { it.copy(isGenerating = false) }
                    // Re-warm for the next turn (cheap: engine + session dedupe).
                    prepareSession()
                }
            }
    }

    fun stopGeneration() {
        generateJob?.cancel()
        generateJob = null
    }

    private suspend fun runGeneration(agent: Agent?, model: LlmModel, text: String) {
        val effectiveModel = loadEngineWithFallback(model) ?: return

        // Conversation row (history) exists before the first token. Persist per-chat customization
        // for an agent-less chat so it survives reopen.
        val conversationId =
            _state.value.conversationId ?: run {
                val s = _state.value
                val created =
                    conversations.create(
                        agentId = agent?.id,
                        modelId = effectiveModel.id,
                        customToolIds = if (agent == null) s.customToolIds else null,
                        customSkillIds = if (agent == null) s.customSkillIds else null,
                        customSystemPrompt = if (agent == null) s.customSystemPrompt else null,
                    )
                _state.update { it.copy(conversationId = created.id) }
                created.id
            }
        conversations.ensureTitle(conversationId, text)

        ensureSession(effectiveModel, agent, conversationId).getOrElse {
            _state.update { s -> s.copy(error = ChatError.Raw(it.message ?: "Could not start conversation")) }
            return
        }

        val userMessage =
            ChatMessage(
                id = newId(),
                conversationId = conversationId,
                role = MessageRole.USER,
                content = text,
                createdAt = epochMillis(),
            )
        conversations.appendMessage(userMessage)
        _state.update { it.copy(messages = it.messages + userMessage) }

        streamAndPersist(effectiveModel, agent, conversationId, text)
    }

    private suspend fun ensureEngineFor(model: LlmModel): Result<Unit> {
        val engineKey = "${model.id}|${model.config.backend.name}|${model.config.maxTokens}"
        val engineConfig =
            EngineConfig(
                modelPath = downloads.modelPath(model),
                backend = if (model.config.backend == LlmBackend.GPU) Backend.Gpu() else Backend.Cpu(),
                maxNumTokens = model.config.maxTokens,
                cacheDir = dirs.cacheDir,
            )
        return runtime.ensureEngine(model.id, engineKey, engineConfig)
    }

    /**
     * Loads the engine for [model], transparently retrying once on the CPU backend when a GPU load
     * fails with a recoverable native error (see [shouldFallBackToCpu] — e.g. the Gemma 4 E-series
     * `llm_litert_compiled_model_executor` failures). A successful CPU fallback is persisted and a
     * [ChatNotice.SWITCHED_TO_CPU] notice is shown. Returns the effective model to use for the
     * conversation, or null after setting [ChatSessionState.error] on failure.
     */
    private suspend fun loadEngineWithFallback(model: LlmModel): LlmModel? {
        val first = ensureEngineFor(model)
        if (first.isSuccess) return model

        val cause = first.exceptionOrNull()
        if (!shouldFallBackToCpu(model.config.backend, cause?.message)) {
            _state.update { it.copy(error = loadError(cause?.message)) }
            return null
        }

        log.w { "GPU load failed for ${model.id}; retrying on CPU: ${cause?.message}" }
        val cpuModel = model.copy(config = model.config.copy(backend = LlmBackend.CPU))
        if (ensureEngineFor(cpuModel).isFailure) {
            _state.update { it.copy(error = ChatError.Known(ChatErrorKind.MODEL_UNSUPPORTED_ON_DEVICE)) }
            return null
        }
        models.updateConfig(model.id, cpuModel.config)
        _state.update { it.copy(notice = ChatNotice.SWITCHED_TO_CPU, error = null) }
        return cpuModel
    }

    private suspend fun streamAndPersist(
        model: LlmModel,
        agent: Agent?,
        conversationId: String,
        text: String,
    ) {
        val startedAt = epochMillis()
        var firstTokenAt = 0L
        var charCount = 0
        var completed = false

        try {
            runtime.generate(sessionHandle!!, text).collect { event ->
                when (event) {
                    is GenEvent.Chunk -> {
                        if (firstTokenAt == 0L) firstTokenAt = epochMillis()
                        charCount += event.text.length
                        _state.update { it.copy(streamingText = it.streamingText + event.text) }
                    }

                    is GenEvent.Thought -> {
                        if (firstTokenAt == 0L) firstTokenAt = epochMillis()
                        charCount += event.text.length
                        _state.update { it.copy(streamingThought = it.streamingThought + event.text) }
                    }

                    is GenEvent.Done -> {
                        completed = true
                        finishAssistantMessage(conversationId, startedAt, firstTokenAt, charCount)
                    }

                    is GenEvent.Error -> {
                        completed = true
                        finishAssistantMessage(conversationId, startedAt, firstTokenAt, charCount)
                        handleGenerationError(model, hadOutput = charCount > 0, message = event.message)
                    }
                }
            }
        } catch (e: CancellationException) {
            // Stop pressed: keep whatever streamed so far. NonCancellable so the
            // persistence writes still run inside the cancelled coroutine.
            if (!completed) {
                withContext(NonCancellable) {
                    finishAssistantMessage(conversationId, startedAt, firstTokenAt, charCount)
                    touchConversation(conversationId, agent, model)
                }
            }
            throw e
        }
        touchConversation(conversationId, agent, model)
    }

    private suspend fun touchConversation(conversationId: String, agent: Agent?, model: LlmModel) {
        val s = _state.value
        conversations.touch(
            id = conversationId,
            agentId = agent?.id,
            modelId = model.id,
            customToolIds = if (agent == null) s.customToolIds else null,
            customSkillIds = if (agent == null) s.customSkillIds else null,
            customSystemPrompt = if (agent == null) s.customSystemPrompt else null,
        )
    }

    private suspend fun finishAssistantMessage(
        conversationId: String,
        startedAt: Long,
        firstTokenAt: Long,
        charCount: Int,
    ) {
        val s = _state.value
        if (s.streamingText.isBlank() && s.streamingThought.isBlank()) {
            _state.update { it.copy(streamingText = "", streamingThought = "") }
            return
        }
        val now = epochMillis()
        val streamMillis = (now - (if (firstTokenAt > 0) firstTokenAt else startedAt)).coerceAtLeast(1)
        val tokensPerSec = (charCount / 4.0) / (streamMillis / 1000.0)
        val message =
            ChatMessage(
                id = newId(),
                conversationId = conversationId,
                role = MessageRole.ASSISTANT,
                content = s.streamingText.trim(),
                thought = s.streamingThought.trim(),
                genMillis = now - startedAt,
                tokensPerSec = tokensPerSec,
                createdAt = now,
            )
        conversations.appendMessage(message)
        _state.update {
            it.copy(messages = it.messages + message, streamingText = "", streamingThought = "")
        }
    }

    /**
     * Surfaces a generation failure. When a GPU model fails before producing any output with a
     * recoverable native error, the model is transparently switched to CPU (persisted) and a
     * [ChatNotice.SWITCHED_TO_CPU] is shown so the next send runs on CPU; otherwise the error is
     * mapped to a user-facing [ChatError].
     */
    private suspend fun handleGenerationError(model: LlmModel, hadOutput: Boolean, message: String) {
        if (!hadOutput && shouldFallBackToCpu(model.config.backend, message)) {
            log.w { "GPU generation failed for ${model.id}; switching to CPU: $message" }
            models.updateConfig(model.id, model.config.copy(backend = LlmBackend.CPU))
            _state.update { it.copy(notice = ChatNotice.SWITCHED_TO_CPU, error = null) }
            return
        }
        _state.update { it.copy(error = loadError(message)) }
    }

    // ----------------------------------------------------------- session

    private suspend fun ensureSession(model: LlmModel, agent: Agent?, conversationId: String?): Result<Unit> =
        sessionMutex.withLock {
            val skillsForAgent = effectiveSkills(agent)
            val toolIds = effectiveToolIds(agent, skillsForAgent)
            val systemPrompt = buildSystemPrompt(agent, skillsForAgent)
            val signature = sessionSignatureOf(model, agent, conversationId, toolIds, skillsForAgent, systemPrompt)
            if (sessionHandle != null && sessionSignature == signature) return@withLock Result.success(Unit)

            closeSession()

            val history = _state.value.messages.filter { it.role != MessageRole.TOOL }
            val config =
                ConversationConfig(
                    systemMessage = systemPrompt?.let { Message.system(it) },
                    initialMessages = historyMessages(history),
                    tools = sessionTools(toolIds, skillsForAgent),
                    samplerConfig =
                        SamplerConfig(
                            topK = model.config.topK,
                            topP = model.config.topP,
                            temperature = model.config.temperature,
                        ),
                )
            runtime.createConversation(config).map { handle ->
                sessionHandle = handle
                sessionSignature = signature
            }
        }

    private fun closeSession() {
        sessionHandle?.let { runtime.closeConversation(it) }
        sessionHandle = null
        sessionSignature = null
    }

    private fun effectiveSkills(agent: Agent?): List<Skill> {
        val ids = if (agent != null) agent.skillIds else _state.value.customSkillIds.orEmpty()
        return ids.mapNotNull { skills.byId(it) }.filter { it.enabled }
    }

    private fun effectiveToolIds(agent: Agent?, skillsForAgent: List<Skill>): List<String> {
        val base =
            when {
                agent != null -> agent.toolIds

                // No agent: per-chat custom selection, else all globally enabled tools.
                _state.value.customToolIds != null -> _state.value.customToolIds.orEmpty()

                else -> selectableTools.map { it.id }
            }
        val enabled =
            base.filter { id ->
                settings.isToolEnabled(id) && selectableTools.any { it.id == id } && id != LoadSkillTool.ID
            }
        return if (skillsForAgent.isNotEmpty()) enabled + LoadSkillTool.ID else enabled
    }

    /**
     * Builds the executable tools for this session in [toolIds] order, wiring each one's invocation
     * sink to the chat recorder. load_skill is created here with the session's [skillsForAgent].
     */
    private fun sessionTools(toolIds: List<String>, skillsForAgent: List<Skill>): List<Tool> =
        buildList {
            toolIds.forEach { id ->
                when (id) {
                    LoadSkillTool.ID -> {
                        add(LoadSkillTool(skillsForAgent).apply { onInvoked = ::recordToolInvocation })
                    }

                    else -> {
                        selectableTools.firstOrNull { it.id == id }?.let { tool ->
                            tool.onInvoked = ::recordToolInvocation
                            add(tool)
                        }
                    }
                }
            }
        }

    @Suppress("LongParameterList") // All inputs that distinguish one native session from another.
    private fun sessionSignatureOf(
        model: LlmModel,
        agent: Agent?,
        conversationId: String?,
        toolIds: List<String>,
        skillsForAgent: List<Skill>,
        systemPrompt: String?,
    ): String =
        buildString {
            append(conversationId)
            append('|').append(model.id)
            append('|').append(model.config.backend).append(model.config.maxTokens)
            append('|').append(model.config.topK).append(model.config.topP).append(model.config.temperature)
            append('|').append(agent?.id).append(':').append(agent?.updatedAt)
            append('|').append(toolIds.sorted().joinToString(","))
            append('|').append(skillsForAgent.joinToString(",") { "${it.id}:${it.updatedAt}" })
            append('|').append(systemPrompt?.hashCode() ?: 0)
        }

    private fun buildSystemPrompt(agent: Agent?, skillsForAgent: List<Skill>): String? {
        val parts = mutableListOf<String>()
        val basePrompt = if (agent != null) agent.systemPrompt else _state.value.customSystemPrompt
        basePrompt?.takeIf { it.isNotBlank() }?.let { parts += it.trim() }
        if (skillsForAgent.isNotEmpty()) {
            parts +=
                buildString {
                    appendLine("# Skills")
                    appendLine(
                        "You have the following optional skills. Before applying a skill, " +
                            "call the load_skill tool with its exact name to get its full instructions.",
                    )
                    for (skill in skillsForAgent) {
                        appendLine("- ${skill.name}: ${skill.description.ifBlank { "(no description)" }}")
                    }
                }.trim()
        }
        return parts.joinToString("\n\n").ifBlank { null }
    }

    private fun historyMessages(history: List<ChatMessage>): List<Message> =
        history.mapNotNull { m ->
            when {
                m.content.isBlank() -> null
                m.role == MessageRole.USER -> Message.user(m.content)
                else -> Message.model(m.content)
            }
        }

    // ----------------------------------------------------------- tool events

    /**
     * Called by each tool after it runs (inside the conversation's tool loop, off the model's
     * generation). Builds a TOOL message and hands it to the tool-message flow, whose single
     * collector persists and appends it in order.
     */
    private fun recordToolInvocation(toolName: String, argsJson: String, resultJson: String) {
        val s = _state.value
        val conversationId = s.conversationId ?: return
        if (!s.isGenerating) return
        toolMessages.tryEmit(
            ChatMessage(
                id = newId(),
                conversationId = conversationId,
                role = MessageRole.TOOL,
                toolName = toolName,
                toolArgs = argsJson,
                toolResult = resultJson,
                createdAt = epochMillis(),
            ),
        )
    }
}
