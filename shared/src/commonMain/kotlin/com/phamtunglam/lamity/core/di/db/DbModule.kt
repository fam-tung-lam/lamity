package com.phamtunglam.lamity.core.di.db

import com.phamtunglam.lamity.core.data.DatabaseSeeder
import com.phamtunglam.lamity.core.data.db.LamityDatabase
import com.phamtunglam.lamity.core.data.db.buildLamityDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

val dbModule: Module =
    module {
        single<LamityDatabase> { buildLamityDatabase(get()) }
        single { get<LamityDatabase>().modelsDao() }
        single { get<LamityDatabase>().agentsDao() }
        single { get<LamityDatabase>().skillsDao() }
        single { get<LamityDatabase>().toolsDao() }
        single { get<LamityDatabase>().conversationsDao() }

        // Seeds first-launch data (models → tools → skills → agents) once, at startup.
        single(createdAtStart = true) {
            DatabaseSeeder(
                models = get(),
                tools = get(),
                skills = get(),
                agents = get(),
                builtinTools = get(),
                scope = get(),
            )
        }
    }
