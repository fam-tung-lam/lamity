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
    version = 1,
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
 * place. While the app is in development the schema isn't migrated — any version change drops and
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
