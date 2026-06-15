package com.phamtunglam.lamity.unitTests.feature.agents.data

import com.phamtunglam.lamity.core.data.db.daos.AgentsDao
import com.phamtunglam.lamity.core.data.db.entities.AgentConfigEntity
import com.phamtunglam.lamity.core.data.db.entities.AgentEntity
import com.phamtunglam.lamity.core.data.db.entities.SkillEntity
import com.phamtunglam.lamity.core.data.db.entities.ToolEntity
import com.phamtunglam.lamity.core.data.db.relations.AgentWithRelations
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
import io.kotest.core.spec.style.BehaviorSpec
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

        // The database is the single source of truth: the mocked DAO is backed by a StateFlow of
        // relation graphs that upsertGraph writes and the repository observes.
        suspend fun createRepository(): AgentsRepository {
            val backing = MutableStateFlow<List<AgentWithRelations>>(emptyList())
            every { dao.observeAllWithRelations() } returns backing
            everySuspend { dao.getAllWithRelations() } calls { backing.value }
            everySuspend { dao.upsertGraph(any(), any(), any(), any()) } calls { args ->
                val agent = args.arg<AgentEntity>(0)
                val config = args.arg<AgentConfigEntity?>(1)
                val skillIds = args.arg<List<String>>(2)
                val toolIds = args.arg<List<String>>(3)
                val row =
                    AgentWithRelations(
                        agent = agent,
                        model = null,
                        config = config,
                        skills = skillIds.map { SkillEntity(it, "", "", "", 0, 0) },
                        tools = toolIds.map { ToolEntity(it, "", "") },
                    )
                backing.value = backing.value.filterNot { it.agent.id == agent.id } + row
            }
            everySuspend { dao.delete(any()) } calls { (id: String) ->
                backing.value = backing.value.filterNot { it.agent.id == id }
            }
            val repository = AgentsRepositoryImpl(dao, detachedTestScope())
            repository.awaitLoaded()
            return repository
        }

        Given("an agents table") {
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
                            modelId = "model-1",
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
                            modelId = "model-1",
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

            When("an agent is deleted") {
                Then("it disappears from the repository") {
                    val repository = createRepository()
                    val created =
                        repository.upsert(
                            id = null,
                            name = "Researcher",
                            description = "",
                            systemPrompt = "",
                            toolIds = emptyList(),
                            skillIds = emptyList(),
                            modelId = "model-1",
                            modelConfig = null,
                        )
                    advanceUntilIdle()

                    repository.delete(created.id)
                    advanceUntilIdle()

                    repository.byId(created.id) shouldBe null
                }
            }
        }
    })
