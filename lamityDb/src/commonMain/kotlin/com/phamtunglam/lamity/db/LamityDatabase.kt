package com.phamtunglam.lamity.db

import androidx.room3.AutoMigration
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.DeleteColumn
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.migration.AutoMigrationSpec
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.phamtunglam.lamity.db.daos.AgentsDao
import com.phamtunglam.lamity.db.daos.ConversationsDao
import com.phamtunglam.lamity.db.daos.ModelsDao
import com.phamtunglam.lamity.db.daos.SettingsDao
import com.phamtunglam.lamity.db.daos.SkillsDao
import com.phamtunglam.lamity.db.entities.AgentEntity
import com.phamtunglam.lamity.db.entities.ConversationEntity
import com.phamtunglam.lamity.db.entities.MessageEntity
import com.phamtunglam.lamity.db.entities.ModelEntity
import com.phamtunglam.lamity.db.entities.SettingsEntity
import com.phamtunglam.lamity.db.entities.SkillEntity
import kotlinx.coroutines.Dispatchers

const val LAMITY_DB_FILE_NAME = "lamity.db"

@Database(
    entities = [
        SettingsEntity::class,
        ModelEntity::class,
        AgentEntity::class,
        SkillEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2), // settings.wifiOnlyDownloads added
        AutoMigration(from = 2, to = 3, spec = LamityDatabase.DropSettingsHfToken::class), // settings.hfToken removed
    ],
)
@ConstructedBy(LamityDatabaseConstructor::class)
abstract class LamityDatabase : RoomDatabase() {
    /** Drops the legacy `settings.hfToken` column; the token is now injected at build time. */
    @DeleteColumn(tableName = "settings", columnName = "hfToken")
    class DropSettingsHfToken : AutoMigrationSpec

    abstract fun settingsDao(): SettingsDao
    abstract fun modelsDao(): ModelsDao
    abstract fun agentsDao(): AgentsDao
    abstract fun skillsDao(): SkillsDao
    abstract fun conversationsDao(): ConversationsDao
}

expect object LamityDatabaseConstructor : RoomDatabaseConstructor<LamityDatabase> {
    override fun initialize(): LamityDatabase
}

/**
 * Finishes a platform-created builder with the shared configuration. Platform
 * code provides the builder (it knows the database path / Context); this keeps
 * driver and threading choices in one place.
 */
fun buildLamityDatabase(builder: RoomDatabase.Builder<LamityDatabase>): LamityDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .build()
