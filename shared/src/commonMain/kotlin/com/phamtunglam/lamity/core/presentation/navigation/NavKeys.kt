package com.phamtunglam.lamity.core.presentation.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/** Root: the home screen of cards. */
@Serializable
data object HomeKey : NavKey

/**
 * The active chat (pushed from Chats / Models / new-chat). [conversationId] selects an existing
 * conversation thread to open; null starts a fresh chat with the restored model/agent selection.
 */
@Serializable
data class ChatKey(val conversationId: String? = null) : NavKey

/** Home-card destinations. */
@Serializable
data object ChatsKey : NavKey

@Serializable
data object AgentsKey : NavKey

@Serializable
data object SkillsKey : NavKey

@Serializable
data object ToolsKey : NavKey

@Serializable
data object ModelsKey : NavKey

@Serializable
data object SettingsKey : NavKey

/** Pushed editor destinations. */
@Serializable
data class AgentEditKey(val agentId: String?) : NavKey

@Serializable
data class SkillEditKey(val skillId: String?) : NavKey

/**
 * Back-stack state restoration needs every [NavKey] subtype registered for
 * polymorphic serialization (required by rememberNavBackStack on non-Android
 * platforms).
 */
val navSavedStateConfiguration: SavedStateConfiguration =
    SavedStateConfiguration {
        serializersModule =
            SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(HomeKey::class)
                    subclass(ChatKey::class)
                    subclass(ChatsKey::class)
                    subclass(AgentsKey::class)
                    subclass(SkillsKey::class)
                    subclass(ToolsKey::class)
                    subclass(ModelsKey::class)
                    subclass(SettingsKey::class)
                    subclass(AgentEditKey::class)
                    subclass(SkillEditKey::class)
                }
            }
    }
