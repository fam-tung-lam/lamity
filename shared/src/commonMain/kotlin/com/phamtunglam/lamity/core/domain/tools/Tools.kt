package com.phamtunglam.lamity.core.domain.tools

import com.phamtunglam.lamity.core.domain.platform.PlatformInfo
import com.phamtunglam.lamity.feature.settings.data.SettingsRepository
import com.phamtunglam.lamity.feature.studio.domain.Skill
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

object ToolIds {
    const val GET_CURRENT_TIME = "get_current_time"
    const val CALCULATE = "calculate"
    const val SET_THEME = "set_theme"
    const val SET_LANGUAGE = "set_language"
    const val RANDOM_NUMBER = "random_number"
    const val DEVICE_INFO = "device_info"
    const val LOAD_SKILL = "load_skill"
}

/** Everything a tool implementation may touch. */
class ToolContext(
    val settings: SettingsRepository,
    val platformInfo: PlatformInfo,
    /** App-level scope: tools run on a synchronous native callback, so writes
     *  that hit the data layer are launched here instead of blocking it. */
    val scope: CoroutineScope,
    /** Skills attached to the active chat session; backs the load_skill tool. */
    var activeSkills: () -> List<Skill> = { emptyList() },
)

/** A built-in tool: OpenAPI-style spec + common Kotlin executor. */
class BuiltinTool(
    val id: String,
    val displayName: String,
    val description: String,
    val parameters: JsonObject,
    val execute: (args: JsonObject, ctx: ToolContext) -> JsonElement,
)

/** A tool invocation that happened during generation; shown in chat and persisted. */
data class ToolEvent(
    val id: String,
    val toolId: String,
    val argsJson: String,
    val resultJson: String,
    val atMillis: Long,
)
