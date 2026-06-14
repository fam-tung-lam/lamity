package com.phamtunglam.lamity.core.di

import com.phamtunglam.lamity.db.LamityDatabase
import com.phamtunglam.lamity.db.buildLamityDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

val dbModule: Module = module {
    single<LamityDatabase> { buildLamityDatabase(get()) }
    single { get<LamityDatabase>().modelsDao() }
    single { get<LamityDatabase>().agentsDao() }
    single { get<LamityDatabase>().skillsDao() }
    single { get<LamityDatabase>().conversationsDao() }
}
