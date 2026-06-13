package com.phamtunglam.lamity.unitTests.feature.studio.domain

import com.phamtunglam.lamity.feature.studio.data.AgentsRepository
import com.phamtunglam.lamity.feature.studio.data.SkillsRepository
import com.phamtunglam.lamity.feature.studio.domain.DeleteSkillUseCase
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.resetAnswers
import dev.mokkery.resetCalls
import dev.mokkery.verifySuspend
import io.kotest.core.spec.style.BehaviorSpec

class DeleteSkillUseCaseTest : BehaviorSpec({

    val skills = mock<SkillsRepository>()
    val agents = mock<AgentsRepository>()

    afterEach {
        resetAnswers(skills, agents)
        resetCalls(skills, agents)
    }

    Given("a skill attached to agents") {
        When("the skill is deleted") {
            Then("it is removed and detached from every agent") {
                everySuspend { skills.delete("skill-1") } returns Unit
                everySuspend { agents.detachSkillEverywhere("skill-1") } returns Unit

                DeleteSkillUseCase(skills, agents)("skill-1")

                verifySuspend {
                    skills.delete("skill-1")
                    agents.detachSkillEverywhere("skill-1")
                }
            }
        }
    }
})
