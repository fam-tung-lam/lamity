package com.phamtunglam.lamity.feature.chat.domain.skills

/**
 * The fixed set of built-in skills. Skills are code-defined (like the built-in tools) and surfaced
 * as per-chat on/off toggles in the chat settings sheet; there is no user-facing skill editor.
 */
object BuiltinSkills {
    val all: List<Skill> =
        listOf(
            Skill(
                id = "skill-haiku-mode",
                name = "Haiku Mode",
                description = "Reply to everything as a single haiku.",
                instructions =
                    "From now on, write every answer as exactly one haiku " +
                        "(three lines of 5, 7 and 5 syllables). Do not add anything outside the haiku.",
            ),
            Skill(
                id = "skill-step-by-step-math",
                name = "Step-by-step Math",
                description = "Solve math problems with numbered steps and a verified result.",
                instructions =
                    "When solving any math problem: 1) restate what is asked, " +
                        "2) solve in small numbered steps, 3) use the calculate tool to verify arithmetic, " +
                        "4) end with 'Answer: <result>' on its own line.",
            ),
        )
}
