package com.phamtunglam.lamity.unitTests.feature.skills.domain

import com.phamtunglam.lamity.feature.skills.data.SkillsRepository
import com.phamtunglam.lamity.feature.skills.domain.DeleteSkillUseCase
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.resetAnswers
import dev.mokkery.resetCalls
import dev.mokkery.verifySuspend
import io.kotest.core.spec.style.BehaviorSpec

class DeleteSkillUseCaseTest :
    BehaviorSpec({

        val skills = mock<SkillsRepository>()

        afterEach {
            resetAnswers(skills)
            resetCalls(skills)
        }

        Given("a skill") {
            When("the skill is deleted") {
                Then("it is removed from the repository (agent links cascade in the database)") {
                    everySuspend { skills.delete("skill-1") } returns Unit

                    DeleteSkillUseCase(skills)("skill-1")

                    verifySuspend { skills.delete("skill-1") }
                }
            }
        }
    })
