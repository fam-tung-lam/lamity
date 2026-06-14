package com.phamtunglam.lamity.core.presentation.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/** Root destinations shown in the bottom navigation bar. */
@Serializable
sealed interface TabKey : NavKey

@Serializable
data object ChatKey : TabKey

@Serializable
data object ModelsKey : TabKey

@Serializable
data object HistoryKey : TabKey

@Serializable
data object StudioKey : TabKey

@Serializable
data object SettingsKey : TabKey

/** Pushed editor destinations. */
@Serializable
data class AgentEditKey(val agentId: String?) : NavKey

@Serializable
data class SkillEditKey(val skillId: String?) : NavKey

@Serializable
data class ModelConfigKey(val modelId: String) : NavKey

/**
 * Back-stack state restoration needs every [NavKey] subtype registered for
 * polymorphic serialization (required by rememberNavBackStack on non-Android
 * platforms).
 */
val navSavedStateConfiguration: SavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(ChatKey::class)
            subclass(ModelsKey::class)
            subclass(HistoryKey::class)
            subclass(StudioKey::class)
            subclass(SettingsKey::class)
            subclass(AgentEditKey::class)
            subclass(SkillEditKey::class)
            subclass(ModelConfigKey::class)
        }
    }
}
