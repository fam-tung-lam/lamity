package com.phamtunglam.lamity.core.data.db.relations

import androidx.room3.Embedded
import androidx.room3.Junction
import androidx.room3.Relation
import com.phamtunglam.lamity.core.data.db.entities.AgentConfigEntity
import com.phamtunglam.lamity.core.data.db.entities.AgentEntity
import com.phamtunglam.lamity.core.data.db.entities.AgentSkillCrossRef
import com.phamtunglam.lamity.core.data.db.entities.AgentToolCrossRef
import com.phamtunglam.lamity.core.data.db.entities.ModelEntity
import com.phamtunglam.lamity.core.data.db.entities.SkillEntity
import com.phamtunglam.lamity.core.data.db.entities.ToolEntity

/**
 * An agent with everything it owns: its model (one-to-one via the `modelId` FK), its optional
 * config override (one-to-one), and its skills and tools (many-to-many via junction tables).
 */
data class AgentWithRelations(
    @Embedded val agent: AgentEntity,
    @Relation(parentColumns = ["model_id"], entityColumns = ["id"])
    val model: ModelEntity?,
    @Relation(parentColumns = ["id"], entityColumns = ["agent_id"])
    val config: AgentConfigEntity?,
    @Relation(
        parentColumns = ["id"],
        entityColumns = ["id"],
        associateBy =
            Junction(
                value = AgentSkillCrossRef::class,
                parentColumns = ["agent_id"],
                entityColumns = ["skill_id"],
            ),
    )
    val skills: List<SkillEntity>,
    @Relation(
        parentColumns = ["id"],
        entityColumns = ["id"],
        associateBy =
            Junction(
                value = AgentToolCrossRef::class,
                parentColumns = ["agent_id"],
                entityColumns = ["tool_id"],
            ),
    )
    val tools: List<ToolEntity>,
)
