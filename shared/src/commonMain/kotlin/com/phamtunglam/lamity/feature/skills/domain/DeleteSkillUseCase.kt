package com.phamtunglam.lamity.feature.skills.domain

import com.phamtunglam.lamity.feature.agents.data.AgentsRepository
import com.phamtunglam.lamity.feature.skills.data.SkillsRepository

/** Deletes a skill and detaches it from every agent that referenced it. */
class DeleteSkillUseCase(private val skills: SkillsRepository, private val agents: AgentsRepository) {
    suspend operator fun invoke(skillId: String) {
        skills.delete(skillId)
        agents.detachSkillEverywhere(skillId)
    }
}
