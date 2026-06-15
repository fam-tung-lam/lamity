package com.phamtunglam.lamity.feature.chat.domain

import com.phamtunglam.lamity.feature.agents.domain.Agent
import com.phamtunglam.lamity.feature.models.domain.LlmModel
import com.phamtunglam.lamity.feature.models.domain.ModelConfig
import com.phamtunglam.lamity.feature.skills.data.SkillsRepository
import com.phamtunglam.lamity.feature.skills.domain.Skill
import com.phamtunglam.lamity.feature.tools.domain.AppTool
import com.phamtunglam.lamity.feature.tools.domain.LoadSkillTool
import com.phamtunglam.lamity.llm.model.ConversationConfig
import com.phamtunglam.lamity.llm.model.Message
import com.phamtunglam.lamity.llm.model.SamplerConfig
import com.phamtunglam.lamity.llm.tool.Tool

/**
 * Builds the native conversation config for a chat session and a signature that distinguishes one
 * session from another. Tools and skills are sourced exclusively from the selected agent — an
 * agent-less chat runs a plain model with an optional per-chat system prompt and no tools or skills.
 */
class ChatSessionFactory(
    private val skills: SkillsRepository,
    /** User-selectable built-in tools (everything except the per-session load_skill). */
    private val selectableTools: List<AppTool>,
) {
    /** A built session: its [config] and the [signature] that identifies it for dedupe. */
    data class Built(val signature: String, val config: ConversationConfig)

    @Suppress("LongParameterList") // All inputs that distinguish one native session from another.
    fun build(
        model: LlmModel,
        config: ModelConfig,
        agent: Agent?,
        conversationId: String?,
        customSystemPrompt: String?,
        history: List<ChatMessage>,
        onToolInvoked: (toolName: String, argsJson: String, resultJson: String) -> Unit,
    ): Built {
        val skillsForAgent = effectiveSkills(agent)
        val toolIds = effectiveToolIds(agent, skillsForAgent)
        val systemPrompt = buildSystemPrompt(agent, customSystemPrompt, skillsForAgent)
        val signature =
            sessionSignatureOf(model, config, agent, conversationId, toolIds, skillsForAgent, systemPrompt)
        val conversationConfig =
            ConversationConfig(
                systemMessage = systemPrompt?.let { Message.system(it) },
                initialMessages = historyMessages(history.filter { it.role != MessageRole.TOOL }),
                tools = sessionTools(toolIds, skillsForAgent, onToolInvoked),
                samplerConfig =
                    SamplerConfig(
                        topK = config.topK,
                        topP = config.topP,
                        temperature = config.temperature,
                    ),
            )
        return Built(signature, conversationConfig)
    }

    /** Skills attached to the active agent; an agent-less chat has none. */
    private fun effectiveSkills(agent: Agent?): List<Skill> {
        val ids = agent?.skillIds ?: return emptyList()
        return ids.mapNotNull { skills.byId(it) }
    }

    /** Tools attached to the active agent (plus load_skill when it has skills); none without an agent. */
    private fun effectiveToolIds(agent: Agent?, skillsForAgent: List<Skill>): List<String> {
        if (agent == null) return emptyList()
        val enabled =
            agent.toolIds.filter { id ->
                selectableTools.any { it.id == id } && id != LoadSkillTool.ID
            }
        return if (skillsForAgent.isNotEmpty()) enabled + LoadSkillTool.ID else enabled
    }

    /**
     * Builds the executable tools for this session in [toolIds] order, wiring each one's invocation
     * sink to [onToolInvoked]. load_skill is created here with the session's [skillsForAgent].
     */
    private fun sessionTools(
        toolIds: List<String>,
        skillsForAgent: List<Skill>,
        onToolInvoked: (String, String, String) -> Unit,
    ): List<Tool> =
        buildList {
            toolIds.forEach { id ->
                when (id) {
                    LoadSkillTool.ID -> {
                        add(LoadSkillTool(skillsForAgent).apply { onInvoked = onToolInvoked })
                    }

                    else -> {
                        selectableTools.firstOrNull { it.id == id }?.let { tool ->
                            tool.onInvoked = onToolInvoked
                            add(tool)
                        }
                    }
                }
            }
        }

    @Suppress("LongParameterList") // All inputs that distinguish one native session from another.
    private fun sessionSignatureOf(
        model: LlmModel,
        config: ModelConfig,
        agent: Agent?,
        conversationId: String?,
        toolIds: List<String>,
        skillsForAgent: List<Skill>,
        systemPrompt: String?,
    ): String =
        buildString {
            append(conversationId)
            append('|').append(model.id)
            append('|').append(config.backend).append(config.maxTokens)
            append('|').append(config.topK).append(config.topP).append(config.temperature)
            append('|').append(agent?.id).append(':').append(agent?.updatedAt)
            append('|').append(toolIds.sorted().joinToString(","))
            append('|').append(skillsForAgent.joinToString(",") { "${it.id}:${it.updatedAt}" })
            append('|').append(systemPrompt?.hashCode() ?: 0)
        }

    private fun buildSystemPrompt(agent: Agent?, customSystemPrompt: String?, skillsForAgent: List<Skill>): String? {
        val parts = mutableListOf<String>()
        val basePrompt = if (agent != null) agent.systemPrompt else customSystemPrompt
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
}
