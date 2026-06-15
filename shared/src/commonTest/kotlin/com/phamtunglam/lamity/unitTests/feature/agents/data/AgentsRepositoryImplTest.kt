package com.phamtunglam.lamity.unitTests.feature.agents.data

import com.phamtunglam.lamity.db.daos.AgentsDao
import com.phamtunglam.lamity.db.entities.AgentEntity
import com.phamtunglam.lamity.feature.agents.data.AgentsRepository
import com.phamtunglam.lamity.feature.agents.data.AgentsRepositoryImpl
import com.phamtunglam.lamity.feature.models.domain.LlmBackend
import com.phamtunglam.lamity.feature.models.domain.ModelConfig
import com.phamtunglam.lamity.fixtures.advanceUntilIdle
import com.phamtunglam.lamity.fixtures.detachedTestScope
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.resetAnswers
import dev.mokkery.resetCalls
import dev.mokkery.verifySuspend
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow

class AgentsRepositoryImplTest :
    BehaviorSpec({

        val dao = mock<AgentsDao>()

        afterEach {
            resetAnswers(dao)
            resetCalls(dao)
        }

        // The database is the single source of truth: the mocked DAO is backed by
        // a StateFlow that writes update and the repository observes.
        suspend fun createRepository(): AgentsRepository {
            val backing = MutableStateFlow<List<AgentEntity>>(emptyList())
            every { dao.observeAll() } returns backing
            everySuspend { dao.getAll() } calls { backing.value }
            everySuspend { dao.upsert(any()) } calls { (entity: AgentEntity) ->
                backing.value = backing.value.filterNot { it.id == entity.id } + entity
            }
            everySuspend { dao.upsertAll(any()) } calls { (entities: List<AgentEntity>) ->
                val ids = entities.map { it.id }.toSet()
                backing.value = backing.value.filterNot { it.id in ids } + entities
            }
            everySuspend { dao.delete(any()) } calls { (id: String) ->
                backing.value = backing.value.filterNot { it.id == id }
            }
            val repository = AgentsRepositoryImpl(dao, detachedTestScope())
            repository.awaitLoaded()
            return repository
        }

        Given("an empty agents table") {
            When("the repository loads") {
                Then("it seeds the sample agents") {
                    val repository = createRepository()

                    repository.agents.value.shouldHaveSize(2)
                }
                Then("it persists the seeded agents") {
                    createRepository()
                    advanceUntilIdle()

                    verifySuspend { dao.upsertAll(any()) }
                }
            }

            When("an agent is saved") {
                Then("it trims fields and deduplicates tool ids") {
                    val repository = createRepository()

                    val created =
                        repository.upsert(
                            id = null,
                            name = "  Researcher  ",
                            description = "desc",
                            systemPrompt = "prompt",
                            toolIds = listOf("calculate", "calculate"),
                            skillIds = emptyList(),
                            modelId = null,
                            modelConfig = null,
                        )

                    created.name shouldBe "Researcher"
                    created.toolIds shouldBe listOf("calculate")
                }
                Then("it makes the agent retrievable by id") {
                    val repository = createRepository()

                    val created =
                        repository.upsert(
                            id = null,
                            name = "Researcher",
                            description = "",
                            systemPrompt = "",
                            toolIds = emptyList(),
                            skillIds = emptyList(),
                            modelId = null,
                            modelConfig = null,
                        )
                    advanceUntilIdle()

                    repository.byId(created.id).shouldNotBeNull()
                }
                Then("it round-trips the agent's model and config override") {
                    val repository = createRepository()
                    val config =
                        ModelConfig(
                            backend = LlmBackend.CPU,
                            maxTokens = 1024,
                            topK = 10,
                            topP = 0.5,
                            temperature = 0.3,
                        )

                    val created =
                        repository.upsert(
                            id = null,
                            name = "Researcher",
                            description = "",
                            systemPrompt = "",
                            toolIds = emptyList(),
                            skillIds = emptyList(),
                            modelId = "qwen2.5-1.5b-instruct-q8",
                            modelConfig = config,
                        )
                    advanceUntilIdle()

                    val reloaded = repository.byId(created.id).shouldNotBeNull()
                    reloaded.modelId shouldBe "qwen2.5-1.5b-instruct-q8"
                    reloaded.modelConfig shouldBe config
                }
            }

            When("a skill is detached everywhere") {
                Then("no agent references the skill anymore") {
                    val repository = createRepository()

                    // Seeded agent "Lami" carries skill-haiku-mode.
                    repository.detachSkillEverywhere("skill-haiku-mode")
                    advanceUntilIdle()

                    repository.agents.value.forEach { agent ->
                        ("skill-haiku-mode" in agent.skillIds) shouldBe false
                    }
                }
            }

            When("an agent is deleted") {
                Then("it disappears from the repository") {
                    val repository = createRepository()

                    repository.delete("agent-lami")
                    advanceUntilIdle()

                    repository.byId("agent-lami") shouldBe null
                }
            }
        }
    })
