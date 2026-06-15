package com.phamtunglam.lamity.feature.skills.domain

import com.phamtunglam.lamity.feature.skills.data.SkillsRepository

/** Validates and saves a skill. Returns null when the name is blank. */
class SaveSkillUseCase(private val skills: SkillsRepository) {
    suspend operator fun invoke(
        id: String?,
        name: String,
        description: String,
        instructions: String,
        enabled: Boolean,
    ): Skill? {
        if (name.isBlank()) return null
        return skills.upsert(
            id = id,
            name = name,
            description = description,
            instructions = instructions,
            enabled = enabled,
        )
    }
}
