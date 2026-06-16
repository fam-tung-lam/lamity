package com.phamtunglam.lamity.core.data.db

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.phamtunglam.lamity.core.data.db.converters.RoleConverter
import com.phamtunglam.lamity.core.data.db.daos.ConversationsDao
import com.phamtunglam.lamity.core.data.db.entities.ConversationEntity
import com.phamtunglam.lamity.core.data.db.entities.MessageEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

const val LAMITY_DB_FILE_NAME = "lamity.db"

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
    ],
    // v8: custom models removed. The whole catalog is now code-defined (`ModelCatalog`), so the
    // `models` table is gone and only conversations + messages are persisted. Dropping the table
    // can't be auto-migrated, so an upgrade resets the database.
    version = 8,
    exportSchema = true,
)
@TypeConverters(RoleConverter::class)
@ConstructedBy(LamityDatabaseConstructor::class)
abstract class LamityDatabase : RoomDatabase() {
    abstract fun conversationsDao(): ConversationsDao
}

expect object LamityDatabaseConstructor : RoomDatabaseConstructor<LamityDatabase> {
    override fun initialize(): LamityDatabase
}

/**
 * Finishes a platform-created builder with the shared configuration. Platform code provides the
 * builder (it knows the database path / Context); this keeps driver and threading choices in one
 * place. The schema change can't be auto-migrated, so an upgrade from any prior version drops and
 * recreates the database.
 */
fun buildLamityDatabase(
    builder: RoomDatabase.Builder<LamityDatabase>,
    queryDispatcher: CoroutineDispatcher = Dispatchers.Default,
): LamityDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(queryDispatcher)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
