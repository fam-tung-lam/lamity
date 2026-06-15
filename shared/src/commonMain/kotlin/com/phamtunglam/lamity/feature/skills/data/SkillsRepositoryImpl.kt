package com.phamtunglam.lamity.feature.skills.data

import co.touchlab.kermit.Logger
import com.phamtunglam.lamity.core.data.db.daos.SkillsDao
import com.phamtunglam.lamity.core.data.db.entities.SkillEntity
import com.phamtunglam.lamity.core.domain.platform.epochMillis
import com.phamtunglam.lamity.core.domain.platform.newId
import com.phamtunglam.lamity.feature.skills.domain.Skill
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

/** Skills live in Room; the database is the single source of truth (seeded by DatabaseSeeder). */
class SkillsRepositoryImpl(private val dao: SkillsDao, scope: CoroutineScope) : SkillsRepository {
    private val log = Logger.withTag("SkillsRepository")

    private val loaded = CompletableDeferred<Unit>()

    override val skills: StateFlow<List<Skill>> =
        dao
            .observeAll()
            .map { rows -> rows.map { it.toDomain() } }
            .catch { e ->
                log.e(e) { "failed to observe skills" }
                emit(emptyList())
            }.onEach { if (!loaded.isCompleted) loaded.complete(Unit) }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override suspend fun awaitLoaded() = loaded.await()

    override fun byId(id: String?): Skill? = id?.let { i -> skills.value.firstOrNull { it.id == i } }

    override suspend fun upsert(
        id: String?,
        name: String,
        description: String,
        instructions: String,
    ): Skill {
        val now = epochMillis()
        val existing = byId(id)
        val skill =
            (existing ?: Skill(id = "skill-${newId().take(8)}", name = "", createdAt = now))
                .copy(
                    name = name.trim(),
                    description = description.trim(),
                    instructions = instructions.trim(),
                    updatedAt = now,
                )
        runCatching { dao.upsert(skill.toSkillEntity()) }
            .onFailure { log.e(it) { "failed to persist skill ${skill.id}" } }
        return skill
    }

    override suspend fun delete(skillId: String) {
        // Junction rows in agent_skills cascade-delete with the skill.
        runCatching { dao.delete(skillId) }
            .onFailure { log.e(it) { "failed to delete skill $skillId" } }
    }
}

internal fun SkillEntity.toDomain() =
    Skill(
        id = id,
        name = name,
        description = description,
        instructions = instructions,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun Skill.toSkillEntity() =
    SkillEntity(
        id = id,
        name = name,
        description = description,
        instructions = instructions,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
