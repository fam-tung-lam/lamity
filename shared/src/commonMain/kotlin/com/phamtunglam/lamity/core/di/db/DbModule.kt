package com.phamtunglam.lamity.core.di.db

import com.phamtunglam.lamity.core.data.db.LamityDatabase
import com.phamtunglam.lamity.core.data.db.buildLamityDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

val dbModule: Module =
    module {
        single<LamityDatabase> { buildLamityDatabase(get()) }
        single { get<LamityDatabase>().modelsDao() }
        single { get<LamityDatabase>().conversationsDao() }
    }
