package com.phamtunglam.lamity.feature.tools.domain

import com.phamtunglam.lamity.core.domain.platform.PlatformInfo
import com.phamtunglam.lamity.core.domain.platform.currentTimeInfo
import com.phamtunglam.lamity.feature.localization.data.AppLocaleStore
import com.phamtunglam.lamity.feature.localization.domain.AppLocale
import com.phamtunglam.lamity.feature.settings.data.SettingsRepository
import com.phamtunglam.lamity.feature.settings.domain.ThemeMode
import com.phamtunglam.lamity.feature.skills.domain.Skill
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.put
import kotlin.math.roundToLong
import kotlin.random.Random

/** Reports the current date and time, optionally for a given IANA timezone. */
class GetCurrentTimeTool :
    AppTool(
        id = "get_current_time",
        displayName = "Current time",
        description =
            "Get the current date and time. Optionally pass an IANA timezone id " +
                "such as 'Asia/Ho_Chi_Minh'; defaults to the device timezone.",
        parameters =
            objectSchema {
                put("timezone", propSchema("string", "Optional IANA timezone id, e.g. 'Europe/Paris'."))
            },
    ) {
    override suspend fun perform(arguments: JsonObject): JsonElement {
        val tz = arguments.stringOrNull("timezone")
        val info = currentTimeInfo(tz) ?: return errorJson("Unknown timezone '$tz'")
        return buildJsonObject {
            put("iso", info.iso)
            put("date", info.date)
            put("time", info.time)
            put("day_of_week", info.dayOfWeek)
            put("timezone", info.timeZone)
            put("utc_offset", info.utcOffset)
        }
    }
}

/** Evaluates a math expression. */
class CalculateTool :
    AppTool(
        id = "calculate",
        displayName = "Calculator",
        description =
            "Evaluate a math expression. Supports + - * / % ^, parentheses, pi, e " +
                "and functions sin cos tan sqrt abs ln log exp floor ceil round min max pow. " +
                "Trigonometry uses radians.",
        parameters =
            objectSchema(required = listOf("expression")) {
                put("expression", propSchema("string", "The expression to evaluate, e.g. '2*(3+4)^2'."))
            },
    ) {
    override suspend fun perform(arguments: JsonObject): JsonElement {
        val expression = arguments.stringOrNull("expression") ?: return errorJson("Missing 'expression'")
        val value = Calculator.evaluate(expression)
        return buildJsonObject {
            put("expression", expression)
            put("result", value)
        }
    }
}

/** Changes the app color theme. */
class SetThemeTool(private val settings: SettingsRepository) :
    AppTool(
        id = "set_theme",
        displayName = "Change theme",
        description = "Change the app color theme. Mode must be 'light', 'dark' or 'system'.",
        parameters =
            objectSchema(required = listOf("mode")) {
                put(
                    "mode",
                    propSchema("string", "One of: light, dark, system.", enum = listOf("light", "dark", "system")),
                )
            },
    ) {
    override suspend fun perform(arguments: JsonObject): JsonElement {
        val mode =
            when (arguments.stringOrNull("mode")?.lowercase()) {
                "light" -> ThemeMode.LIGHT
                "dark" -> ThemeMode.DARK
                "system" -> ThemeMode.SYSTEM
                else -> return errorJson("mode must be light, dark or system")
            }
        settings.setThemeMode(mode)
        return buildJsonObject {
            put("ok", true)
            put("theme", mode.name.lowercase())
        }
    }
}

/** Changes the app interface language. */
class SetLanguageTool(private val localeStore: AppLocaleStore) :
    AppTool(
        id = "set_language",
        displayName = "Change language",
        description =
            "Change the app interface language. Supported: 'en' (English), " +
                "'vi' (Tiếng Việt), 'es' (Español), or 'system' to follow the device language.",
        parameters =
            objectSchema(required = listOf("language")) {
                put(
                    "language",
                    propSchema("string", "One of: en, vi, es, system.", enum = listOf("en", "vi", "es", "system")),
                )
            },
    ) {
    override suspend fun perform(arguments: JsonObject): JsonElement {
        val requested = arguments.stringOrNull("language")?.lowercase()
        val locale =
            when (requested) {
                "system" -> {
                    null
                }

                else -> {
                    AppLocale.entries.firstOrNull { it.bcp47 == requested }
                        ?: return errorJson("language must be en, vi, es or system")
                }
            }
        localeStore.setLocale(locale)
        return buildJsonObject {
            put("ok", true)
            put("language", locale?.bcp47 ?: "system")
        }
    }
}

/** Generates a random number between min and max (inclusive). */
class RandomNumberTool :
    AppTool(
        id = "random_number",
        displayName = "Random number",
        description =
            "Generate a random number between min and max (inclusive). " +
                "Defaults: min 1, max 100, integer true.",
        parameters =
            objectSchema {
                put("min", propSchema("number", "Lower bound. Default 1."))
                put("max", propSchema("number", "Upper bound. Default 100."))
                put("integer", propSchema("boolean", "Return an integer when true. Default true."))
            },
    ) {
    override suspend fun perform(arguments: JsonObject): JsonElement {
        val min = arguments.doubleOrNull("min") ?: 1.0
        val max = arguments.doubleOrNull("max") ?: 100.0
        if (min > max) return errorJson("min must be <= max")
        val integer = arguments.booleanOrNull("integer") ?: true
        val raw = min + Random.nextDouble() * (max - min)
        return buildJsonObject {
            if (integer) put("value", raw.roundToLong()) else put("value", raw)
            put("min", min)
            put("max", max)
        }
    }
}

/** Reports information about the device this app runs on. */
class DeviceInfoTool(private val platformInfo: PlatformInfo) :
    AppTool(
        id = "device_info",
        displayName = "Device info",
        description =
            "Get information about the device this app runs on: platform, " +
                "OS version and device model.",
        parameters = objectSchema { },
    ) {
    override suspend fun perform(arguments: JsonObject): JsonElement =
        buildJsonObject {
            put("platform", platformInfo.platform)
            put("os_version", platformInfo.osVersion)
            put("device_model", platformInfo.deviceModel)
        }
}

/**
 * Loads a named skill's full instructions. Constructed per chat session with the skills attached to
 * that session, so it resolves names against exactly those skills.
 */
class LoadSkillTool(private val skills: List<Skill>) :
    AppTool(
        id = ID,
        displayName = "Load skill",
        description =
            "Load the full instructions of an available skill by its exact name. " +
                "Always call this before applying a skill.",
        parameters =
            objectSchema(required = listOf("skill_name")) {
                put("skill_name", propSchema("string", "Exact name of the skill to load."))
            },
    ) {
    override suspend fun perform(arguments: JsonObject): JsonElement {
        val name = arguments.stringOrNull("skill_name")?.trim() ?: return errorJson("Missing 'skill_name'")
        val skill =
            skills.firstOrNull { it.name.equals(name, ignoreCase = true) }
                ?: return buildJsonObject {
                    put("error", "Skill '$name' not found")
                    put("available", JsonArray(skills.map { JsonPrimitive(it.name) }))
                }
        return buildJsonObject {
            put("name", skill.name)
            put("description", skill.description)
            put("instructions", skill.instructions)
        }
    }

    companion object {
        /** Tool id; referenced by the chat session to attach load_skill only when skills exist. */
        const val ID = "load_skill"
    }
}

// ------------------------------------------------------------- json helpers

private fun objectSchema(required: List<String> = emptyList(), properties: JsonObjectBuilder.() -> Unit): JsonObject =
    buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject(properties))
        if (required.isNotEmpty()) {
            put("required", JsonArray(required.map { JsonPrimitive(it) }))
        }
    }

private fun propSchema(type: String, description: String, enum: List<String>? = null): JsonObject =
    buildJsonObject {
        put("type", type)
        put("description", description)
        if (enum != null) put("enum", JsonArray(enum.map { JsonPrimitive(it) }))
    }

private fun errorJson(message: String): JsonObject = buildJsonObject { put("error", message) }

private fun JsonObject.stringOrNull(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

private fun JsonObject.doubleOrNull(key: String): Double? = (this[key] as? JsonPrimitive)?.doubleOrNull

private fun JsonObject.booleanOrNull(key: String): Boolean? = (this[key] as? JsonPrimitive)?.booleanOrNull
