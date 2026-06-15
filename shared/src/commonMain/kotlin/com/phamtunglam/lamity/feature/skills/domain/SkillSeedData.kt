package com.phamtunglam.lamity.feature.skills.domain

/** Sample skills created on first launch. */
object SkillSeedData {
    fun sampleSkills(now: Long): List<Skill> =
        listOf(
            Skill(
                id = "skill-haiku-mode",
                name = "Haiku Mode",
                description = "Reply to everything as a single haiku.",
                instructions =
                    "From now on, write every answer as exactly one haiku " +
                        "(three lines of 5, 7 and 5 syllables). Do not add anything outside the haiku.",
                enabled = true,
                createdAt = now,
                updatedAt = now,
            ),
            Skill(
                id = "skill-step-by-step-math",
                name = "Step-by-step Math",
                description = "Solve math problems with numbered steps and a verified result.",
                instructions =
                    "When solving any math problem: 1) restate what is asked, " +
                        "2) solve in small numbered steps, 3) use the calculate tool to verify arithmetic, " +
                        "4) end with 'Answer: <result>' on its own line.",
                enabled = true,
                createdAt = now,
                updatedAt = now,
            ),
        )
}
