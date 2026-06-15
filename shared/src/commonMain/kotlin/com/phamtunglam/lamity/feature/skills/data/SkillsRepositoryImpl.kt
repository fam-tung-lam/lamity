package com.phamtunglam.lamity.feature.skills.data

import co.touchlab.kermit.Logger
import com.phamtunglam.lamity.core.domain.platform.epochMillis
import com.phamtunglam.lamity.core.domain.platform.newId
import com.phamtunglam.lamity.db.daos.SkillsDao
import com.phamtunglam.lamity.db.entities.SkillEntity
import com.phamtunglam.lamity.feature.skills.domain.Skill
import com.phamtunglam.lamity.feature.skills.domain.SkillSeedData
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Skills live in Room; the database is the single source of truth. */
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
            }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    init {
        scope.launch {
            val seeded =
                runCatching {
                    if (dao.getAll().isEmpty()) {
                        dao.upsertAll(SkillSeedData.sampleSkills(epochMillis()).map { it.toEntity() })
                    }
                    true
                }.onFailure { log.e(it) { "failed to seed skills" } }.getOrDefault(false)
            // Loaded once the flow reflects the stored (or just-seeded) skills.
            if (seeded) skills.first { it.isNotEmpty() }
            loaded.complete(Unit)
        }
    }

    override suspend fun awaitLoaded() = loaded.await()

    override fun byId(id: String?): Skill? = id?.let { i -> skills.value.firstOrNull { it.id == i } }

    override suspend fun upsert(
        id: String?,
        name: String,
        description: String,
        instructions: String,
        enabled: Boolean,
    ): Skill {
        val now = epochMillis()
        val existing = byId(id)
        val skill =
            (existing ?: Skill(id = "skill-${newId().take(8)}", name = "", createdAt = now))
                .copy(
                    name = name.trim(),
                    description = description.trim(),
                    instructions = instructions.trim(),
                    enabled = enabled,
                    updatedAt = now,
                )
        runCatching { dao.upsert(skill.toEntity()) }
            .onFailure { log.e(it) { "failed to persist skill ${skill.id}" } }
        return skill
    }

    override suspend fun setEnabled(skillId: String, enabled: Boolean) {
        val skill = byId(skillId) ?: return
        runCatching { dao.upsert(skill.copy(enabled = enabled, updatedAt = epochMillis()).toEntity()) }
            .onFailure { log.e(it) { "failed to persist skill $skillId" } }
    }

    override suspend fun delete(skillId: String) {
        runCatching { dao.delete(skillId) }
            .onFailure { log.e(it) { "failed to delete skill $skillId" } }
    }
}

private fun SkillEntity.toDomain() =
    Skill(
        id = id,
        name = name,
        description = description,
        instructions = instructions,
        enabled = enabled,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun Skill.toEntity() =
    SkillEntity(
        id = id,
        name = name,
        description = description,
        instructions = instructions,
        enabled = enabled,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
