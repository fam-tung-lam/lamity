package com.phamtunglam.lamity.core.i18n

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * UI strings, switched at runtime (Settings screen or the set_language tool).
 * Language bundles live in StringsEn/StringsVi/StringsEs; non-English bundles
 * copy [EnStrings] so missing translations fall back to English.
 */
data class Strings(
    val tabChat: String,
    val tabModels: String,
    val tabHistory: String,
    val tabStudio: String,
    val tabSettings: String,

    val chatPlaceholder: String,
    val chatEmptyTitle: String,
    val chatEmptyBody: String,
    val noModelTitle: String,
    val noModelBody: String,
    val goToModels: String,
    val newChat: String,
    val agentLabel: String,
    val noAgent: String,
    val modelLabel: String,
    val stop: String,
    val send: String,
    val loadingModel: String,
    val thinkingLabel: String,
    val toolCallLabel: String,
    val dismiss: String,
    val retry: String,

    val download: String,
    val cancel: String,
    val delete: String,
    val configure: String,
    val downloaded: String,
    val pause: String,
    val resume: String,
    val paused: String,
    val verifying: String,
    val downloadQueued: String,
    val addCustomModel: String,
    val customModelName: String,
    val customModelUrl: String,
    val requiresAuthLabel: String,
    val add: String,
    val deleteModelFileQ: String,
    val removeFromCatalog: String,
    val chatAction: String,
    val needsToken: String,

    val modelConfigTitle: String,
    val backend: String,
    val maxTokens: String,
    val topK: String,
    val topP: String,
    val temperature: String,
    val save: String,
    val resetDefaults: String,
    val configNote: String,

    val historyEmptyTitle: String,
    val historyEmptyBody: String,
    val rename: String,
    val renameConversation: String,
    val deleteConversationQ: String,

    val agentsTab: String,
    val skillsTab: String,
    val toolsTab: String,
    val newAgent: String,
    val newSkill: String,
    val agentName: String,
    val agentDescription: String,
    val systemPrompt: String,
    val attachedTools: String,
    val attachedSkills: String,
    val skillName: String,
    val skillDescription: String,
    val skillInstructions: String,
    val enabled: String,
    val disabledSuffix: String,
    val deleteAgentQ: String,
    val deleteSkillQ: String,
    val edit: String,
    val toolsCaption: String,
    val skillsCaption: String,
    val agentsCaption: String,
    val nameRequired: String,
    val toolsCount: String,
    val skillsCount: String,

    val theme: String,
    val themeLight: String,
    val themeDark: String,
    val themeSystem: String,
    val language: String,
    val downloadsSection: String,
    val wifiOnly: String,
    val wifiOnlyHint: String,
    val hfToken: String,
    val hfTokenHint: String,
    val about: String,

    val back: String,
    val confirmDeleteTitle: String,
)

fun stringsFor(language: String): Strings = when (language) {
    "vi" -> ViStrings
    "es" -> EsStrings
    else -> EnStrings
}

val LocalStrings = staticCompositionLocalOf { EnStrings }
