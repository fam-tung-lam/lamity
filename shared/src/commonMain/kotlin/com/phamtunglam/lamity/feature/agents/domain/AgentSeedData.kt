package com.phamtunglam.lamity.feature.agents.domain

/** Sample agents created on first launch. */
object AgentSeedData {
    fun sampleAgents(now: Long): List<Agent> =
        listOf(
            Agent(
                id = "agent-lami",
                name = "Lami",
                description = "Friendly on-device assistant that uses every built-in tool.",
                systemPrompt =
                    "You are Lami, a concise and friendly assistant running fully " +
                        "on-device. Prefer using your tools when they give a more accurate answer " +
                        "(time, math, device facts, app appearance). Keep answers short.",
                toolIds =
                    listOf(
                        "get_current_time",
                        "calculate",
                        "set_theme",
                        "set_language",
                        "random_number",
                        "device_info",
                    ),
                skillIds = listOf("skill-haiku-mode"),
                modelId = "qwen2.5-1.5b-instruct-q8",
                createdAt = now,
                updatedAt = now,
            ),
            Agent(
                id = "agent-math-tutor",
                name = "Math Tutor",
                description = "Patient tutor that explains math step by step.",
                systemPrompt =
                    "You are a patient math tutor. Explain concepts simply and " +
                        "verify every calculation with the calculate tool before stating it.",
                toolIds = listOf("calculate"),
                skillIds = listOf("skill-step-by-step-math"),
                modelId = "qwen2.5-1.5b-instruct-q8",
                createdAt = now,
                updatedAt = now,
            ),
        )
}
