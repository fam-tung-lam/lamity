@file:OptIn(ExperimentalForeignApi::class)

package com.phamtunglam.lamity.core.data.db

import androidx.room3.Room
import androidx.room3.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUserDomainMask

/** Room builder for the app database, stored under Application Support. */
fun lamityDatabaseBuilder(): RoomDatabase.Builder<LamityDatabase> {
    val baseDir =
        NSSearchPathForDirectoriesInDomains(
            NSApplicationSupportDirectory,
            NSUserDomainMask,
            true,
        ).firstOrNull() as? String ?: NSTemporaryDirectory()
    val dbDir = "$baseDir/databases"
    FileSystem.SYSTEM.createDirectories(dbDir.toPath())
    return Room.databaseBuilder<LamityDatabase>(name = "$dbDir/$LAMITY_DB_FILE_NAME")
}
