package com.phamtunglam.lamity.core.domain.tools

import com.phamtunglam.lamity.core.domain.platform.currentTimeInfo
import com.phamtunglam.lamity.feature.settings.domain.ThemeMode
import kotlin.math.roundToLong
import kotlin.random.Random
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

class ToolRegistry(private val ctx: ToolContext) {

    /** Tools the user can attach to agents and toggle globally (excludes load_skill). */
    val userSelectable: List<BuiltinTool>

    /** All tools including the implicit load_skill. */
    val all: List<BuiltinTool>

    init {
        userSelectable = listOf(
            getCurrentTime(), calculate(), setTheme(), setLanguage(), randomNumber(), deviceInfo(),
        )
        all = userSelectable + loadSkill()
    }

    fun byId(id: String): BuiltinTool? = all.firstOrNull { it.id == id }

    fun execute(toolId: String, args: JsonObject): JsonElement {
        val tool = byId(toolId)
            ?: return error("Unknown tool '$toolId'")
        return runCatching { tool.execute(args, ctx) }
            .getOrElse { error(it.message ?: "Tool execution failed") }
    }

    /** JSON array of OpenAPI-style specs for the given tool ids, consumed by the bridges. */
    fun specsJsonFor(toolIds: List<String>): String {
        val specs = buildJsonArray {
            for (id in toolIds) {
                val tool = byId(id) ?: continue
                add(buildJsonObject {
                    put("name", tool.id)
                    put("description", tool.description)
                    put("parameters", tool.parameters)
                })
            }
        }
        return specs.toString()
    }

    private fun error(message: String): JsonObject =
        buildJsonObject { put("error", message) }

    // ---------------------------------------------------------------- tools

    private fun getCurrentTime() = BuiltinTool(
        id = ToolIds.GET_CURRENT_TIME,
        displayName = "Current time",
        description = "Get the current date and time. Optionally pass an IANA timezone id " +
            "such as 'Asia/Ho_Chi_Minh'; defaults to the device timezone.",
        parameters = objectSchema {
            put("timezone", propSchema("string", "Optional IANA timezone id, e.g. 'Europe/Paris'."))
        },
    ) { args, _ ->
        val tz = args.stringOrNull("timezone")
        val info = currentTimeInfo(tz)
            ?: return@BuiltinTool error("Unknown timezone '$tz'")
        buildJsonObject {
            put("iso", info.iso)
            put("date", info.date)
            put("time", info.time)
            put("day_of_week", info.dayOfWeek)
            put("timezone", info.timeZone)
            put("utc_offset", info.utcOffset)
        }
    }

    private fun calculate() = BuiltinTool(
        id = ToolIds.CALCULATE,
        displayName = "Calculator",
        description = "Evaluate a math expression. Supports + - * / % ^, parentheses, pi, e " +
            "and functions sin cos tan sqrt abs ln log exp floor ceil round min max pow. " +
            "Trigonometry uses radians.",
        parameters = objectSchema(required = listOf("expression")) {
            put("expression", propSchema("string", "The expression to evaluate, e.g. '2*(3+4)^2'."))
        },
    ) { args, _ ->
        val expression = args.stringOrNull("expression")
            ?: return@BuiltinTool error("Missing 'expression'")
        val value = Calculator.evaluate(expression)
        buildJsonObject {
            put("expression", expression)
            put("result", value)
        }
    }

    private fun setTheme() = BuiltinTool(
        id = ToolIds.SET_THEME,
        displayName = "Change theme",
        description = "Change the app color theme. Mode must be 'light', 'dark' or 'system'.",
        parameters = objectSchema(required = listOf("mode")) {
            put("mode", propSchema("string", "One of: light, dark, system.", enum = listOf("light", "dark", "system")))
        },
    ) { args, ctx ->
        val mode = when (args.stringOrNull("mode")?.lowercase()) {
            "light" -> ThemeMode.LIGHT
            "dark" -> ThemeMode.DARK
            "system" -> ThemeMode.SYSTEM
            else -> return@BuiltinTool error("mode must be light, dark or system")
        }
        ctx.scope.launch { ctx.settings.setThemeMode(mode) }
        buildJsonObject {
            put("ok", true)
            put("theme", mode.name.lowercase())
        }
    }

    private fun setLanguage() = BuiltinTool(
        id = ToolIds.SET_LANGUAGE,
        displayName = "Change language",
        description = "Change the app interface language. Supported: 'en' (English), " +
            "'vi' (Tiếng Việt), 'es' (Español).",
        parameters = objectSchema(required = listOf("language")) {
            put("language", propSchema("string", "One of: en, vi, es.", enum = listOf("en", "vi", "es")))
        },
    ) { args, ctx ->
        val lang = args.stringOrNull("language")?.lowercase()
        if (lang !in setOf("en", "vi", "es")) {
            return@BuiltinTool error("language must be en, vi or es")
        }
        ctx.scope.launch { ctx.settings.setLanguage(lang!!) }
        buildJsonObject {
            put("ok", true)
            put("language", lang)
        }
    }

    private fun randomNumber() = BuiltinTool(
        id = ToolIds.RANDOM_NUMBER,
        displayName = "Random number",
        description = "Generate a random number between min and max (inclusive). " +
            "Defaults: min 1, max 100, integer true.",
        parameters = objectSchema {
            put("min", propSchema("number", "Lower bound. Default 1."))
            put("max", propSchema("number", "Upper bound. Default 100."))
            put("integer", propSchema("boolean", "Return an integer when true. Default true."))
        },
    ) { args, _ ->
        val min = args.doubleOrNull("min") ?: 1.0
        val max = args.doubleOrNull("max") ?: 100.0
        if (min > max) return@BuiltinTool error("min must be <= max")
        val integer = args.booleanOrNull("integer") ?: true
        val raw = min + Random.nextDouble() * (max - min)
        buildJsonObject {
            if (integer) put("value", raw.roundToLong()) else put("value", raw)
            put("min", min)
            put("max", max)
        }
    }

    private fun deviceInfo() = BuiltinTool(
        id = ToolIds.DEVICE_INFO,
        displayName = "Device info",
        description = "Get information about the device this app runs on: platform, " +
            "OS version and device model.",
        parameters = objectSchema { },
    ) { _, ctx ->
        buildJsonObject {
            put("platform", ctx.platformInfo.platform)
            put("os_version", ctx.platformInfo.osVersion)
            put("device_model", ctx.platformInfo.deviceModel)
        }
    }

    private fun loadSkill() = BuiltinTool(
        id = ToolIds.LOAD_SKILL,
        displayName = "Load skill",
        description = "Load the full instructions of an available skill by its exact name. " +
            "Always call this before applying a skill.",
        parameters = objectSchema(required = listOf("skill_name")) {
            put("skill_name", propSchema("string", "Exact name of the skill to load."))
        },
    ) { args, ctx ->
        val name = args.stringOrNull("skill_name")?.trim()
            ?: return@BuiltinTool error("Missing 'skill_name'")
        val skill = ctx.activeSkills().firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: return@BuiltinTool buildJsonObject {
                put("error", "Skill '$name' not found")
                put("available", JsonArray(ctx.activeSkills().map { JsonPrimitive(it.name) }))
            }
        buildJsonObject {
            put("name", skill.name)
            put("description", skill.description)
            put("instructions", skill.instructions)
        }
    }
}

// ------------------------------------------------------------- json helpers

private fun objectSchema(
    required: List<String> = emptyList(),
    properties: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit,
): JsonObject = buildJsonObject {
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

private fun JsonObject.stringOrNull(key: String): String? =
    (this[key] as? JsonPrimitive)?.contentOrNull

private fun JsonObject.doubleOrNull(key: String): Double? =
    (this[key] as? JsonPrimitive)?.doubleOrNull

private fun JsonObject.booleanOrNull(key: String): Boolean? =
    (this[key] as? JsonPrimitive)?.booleanOrNull
