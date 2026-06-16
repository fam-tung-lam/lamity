package com.phamtunglam.lamity.feature.chat.domain

import com.phamtunglam.lamity.feature.models.domain.LlmModel
import com.phamtunglam.lamity.feature.models.domain.ModelConfig
import com.phamtunglam.lamity.feature.skills.domain.BuiltinSkills
import com.phamtunglam.lamity.feature.skills.domain.Skill
import com.phamtunglam.lamity.feature.tools.domain.AppTool
import com.phamtunglam.lamity.feature.tools.domain.LoadSkillTool
import com.phamtunglam.lamity.llm.model.ConversationConfig
import com.phamtunglam.lamity.llm.model.Message
import com.phamtunglam.lamity.llm.model.SamplerConfig
import com.phamtunglam.lamity.llm.tool.Tool

/**
 * Builds the native conversation config for a chat session and a signature that distinguishes one
 * session from another. Tools and skills are the built-ins enabled for this chat (default all), but
 * a model that does not support tools runs as a plain model with neither — only its system prompt.
 */
class ChatSessionFactory(
    /** All built-in tools (everything except the per-session load_skill). */
    private val allTools: List<AppTool>,
) {
    /** A built session: its [config] and the [signature] that identifies it for dedupe. */
    data class Built(val signature: String, val config: ConversationConfig)

    @Suppress("LongParameterList") // All inputs that distinguish one native session from another.
    fun build(
        model: LlmModel,
        config: ModelConfig,
        enabledToolIds: Set<String>,
        enabledSkillIds: Set<String>,
        conversationId: String?,
        customSystemPrompt: String?,
        history: List<ChatMessage>,
        onToolInvoked: (toolName: String, argsJson: String, resultJson: String) -> Unit,
    ): Built {
        val skills = effectiveSkills(model, enabledSkillIds)
        val tools = effectiveTools(model, enabledToolIds)
        val systemPrompt = buildSystemPrompt(customSystemPrompt, skills)
        val toolIds = tools.map { it.id } + if (skills.isNotEmpty()) listOf(LoadSkillTool.ID) else emptyList()
        val signature = sessionSignatureOf(model, config, conversationId, toolIds, skills, systemPrompt)
        val conversationConfig =
            ConversationConfig(
                systemMessage = systemPrompt?.let { Message.system(it) },
                initialMessages = historyMessages(history.filter { it.role != MessageRole.TOOL }),
                tools = sessionTools(tools, skills, onToolInvoked),
                samplerConfig =
                    SamplerConfig(
                        topK = config.topK,
                        topP = config.topP,
                        temperature = config.temperature,
                    ),
            )
        return Built(signature, conversationConfig)
    }

    /** Enabled built-in skills; none when the model can't use tools (skills need load_skill). */
    private fun effectiveSkills(model: LlmModel, enabledSkillIds: Set<String>): List<Skill> {
        if (!model.supportsTools) return emptyList()
        return BuiltinSkills.all.filter { it.id in enabledSkillIds }
    }

    /** Enabled built-in tools; none when the model can't use tools. */
    private fun effectiveTools(model: LlmModel, enabledToolIds: Set<String>): List<AppTool> {
        if (!model.supportsTools) return emptyList()
        return allTools.filter { it.id in enabledToolIds }
    }

    /**
     * Builds the executable tools for this session: the enabled built-ins plus load_skill (created
     * here with this session's [skills]) when there are any skills. Each tool's invocation sink is
     * wired to [onToolInvoked].
     */
    private fun sessionTools(
        tools: List<AppTool>,
        skills: List<Skill>,
        onToolInvoked: (String, String, String) -> Unit,
    ): List<Tool> =
        buildList {
            tools.forEach { tool ->
                tool.onInvoked = onToolInvoked
                add(tool)
            }
            if (skills.isNotEmpty()) {
                add(LoadSkillTool(skills).apply { onInvoked = onToolInvoked })
            }
        }

    @Suppress("LongParameterList") // All inputs that distinguish one native session from another.
    private fun sessionSignatureOf(
        model: LlmModel,
        config: ModelConfig,
        conversationId: String?,
        toolIds: List<String>,
        skills: List<Skill>,
        systemPrompt: String?,
    ): String =
        buildString {
            append(conversationId)
            append('|').append(model.id)
            append('|').append(config.backend).append(config.maxTokens)
            append('|').append(config.topK).append(config.topP).append(config.temperature)
            append('|').append(toolIds.sorted().joinToString(","))
            append('|').append(skills.joinToString(",") { it.id })
            append('|').append(systemPrompt?.hashCode() ?: 0)
        }

    private fun buildSystemPrompt(customSystemPrompt: String?, skills: List<Skill>): String? {
        val parts = mutableListOf<String>()
        customSystemPrompt?.takeIf { it.isNotBlank() }?.let { parts += it.trim() }
        if (skills.isNotEmpty()) {
            parts +=
                buildString {
                    appendLine("# Skills")
                    appendLine(
                        "You have the following optional skills. Before applying a skill, " +
                            "call the load_skill tool with its exact name to get its full instructions.",
                    )
                    for (skill in skills) {
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
