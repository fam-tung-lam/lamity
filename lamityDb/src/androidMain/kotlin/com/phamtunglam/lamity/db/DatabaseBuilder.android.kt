package com.phamtunglam.lamity.db

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase

/** Room builder for the app database, stored in the app's database directory. */
fun lamityDatabaseBuilder(context: Context): RoomDatabase.Builder<LamityDatabase> {
    val appContext = context.applicationContext
    return Room.databaseBuilder<LamityDatabase>(
        context = appContext,
        name = appContext.getDatabasePath(LAMITY_DB_FILE_NAME).absolutePath,
    )
}
