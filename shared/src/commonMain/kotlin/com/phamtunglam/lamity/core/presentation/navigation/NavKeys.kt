package com.phamtunglam.lamity.core.presentation.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/** Root: the chats list, which is the app home. */
@Serializable
data object ChatsKey : NavKey

/**
 * The active chat (pushed from Chats / new-chat). [conversationId] selects an existing conversation
 * thread to open; null starts a fresh chat with the restored model selection.
 */
@Serializable
data class ChatKey(val conversationId: String? = null) : NavKey

/** Model management + selection (download / delete / pick). Pushed from the chat or Settings. */
@Serializable
data object ModelsKey : NavKey

@Serializable
data object SettingsKey : NavKey

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
                    subclass(ChatsKey::class)
                    subclass(ChatKey::class)
                    subclass(ModelsKey::class)
                    subclass(SettingsKey::class)
                }
            }
    }
