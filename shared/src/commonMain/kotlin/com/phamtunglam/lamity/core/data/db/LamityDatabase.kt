package com.phamtunglam.lamity.core.data.db

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.phamtunglam.lamity.core.data.db.converters.BackendConverter
import com.phamtunglam.lamity.core.data.db.converters.RoleConverter
import com.phamtunglam.lamity.core.data.db.daos.AgentsDao
import com.phamtunglam.lamity.core.data.db.daos.ConversationsDao
import com.phamtunglam.lamity.core.data.db.daos.ModelsDao
import com.phamtunglam.lamity.core.data.db.daos.SkillsDao
import com.phamtunglam.lamity.core.data.db.daos.ToolsDao
import com.phamtunglam.lamity.core.data.db.entities.AgentConfigEntity
import com.phamtunglam.lamity.core.data.db.entities.AgentEntity
import com.phamtunglam.lamity.core.data.db.entities.AgentSkillCrossRef
import com.phamtunglam.lamity.core.data.db.entities.AgentToolCrossRef
import com.phamtunglam.lamity.core.data.db.entities.ConversationEntity
import com.phamtunglam.lamity.core.data.db.entities.MessageEntity
import com.phamtunglam.lamity.core.data.db.entities.ModelEntity
import com.phamtunglam.lamity.core.data.db.entities.SkillEntity
import com.phamtunglam.lamity.core.data.db.entities.ToolEntity
import kotlinx.coroutines.Dispatchers

const val LAMITY_DB_FILE_NAME = "lamity.db"

@Database(
    entities = [
        ModelEntity::class,
        AgentEntity::class,
        AgentConfigEntity::class,
        SkillEntity::class,
        ToolEntity::class,
        AgentSkillCrossRef::class,
        AgentToolCrossRef::class,
        ConversationEntity::class,
        MessageEntity::class,
    ],
    // v6: relational redesign (agent ⇄ model/config/skills/tools, conversations decoupled). The
    // schema is rewritten from scratch; existing installs are reset via destructive migration.
    version = 6,
    exportSchema = true,
)
@TypeConverters(RoleConverter::class, BackendConverter::class)
@ConstructedBy(LamityDatabaseConstructor::class)
abstract class LamityDatabase : RoomDatabase() {
    abstract fun modelsDao(): ModelsDao

    abstract fun agentsDao(): AgentsDao

    abstract fun skillsDao(): SkillsDao

    abstract fun toolsDao(): ToolsDao

    abstract fun conversationsDao(): ConversationsDao
}

expect object LamityDatabaseConstructor : RoomDatabaseConstructor<LamityDatabase> {
    override fun initialize(): LamityDatabase
}

/**
 * Finishes a platform-created builder with the shared configuration. Platform code provides the
 * builder (it knows the database path / Context); this keeps driver and threading choices in one
 * place. The schema redesign can't be auto-migrated, so an upgrade from any prior version drops and
 * recreates the database (seed data is re-created on next launch).
 */
fun buildLamityDatabase(builder: RoomDatabase.Builder<LamityDatabase>): LamityDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
