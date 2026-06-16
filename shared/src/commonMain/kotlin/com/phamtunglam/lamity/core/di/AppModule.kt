package com.phamtunglam.lamity.core.di

import com.phamtunglam.lamity.core.di.db.dbModule
import com.phamtunglam.lamity.feature.chat.di.chatModule
import com.phamtunglam.lamity.feature.llmModels.di.modelsModule
import com.phamtunglam.lamity.feature.localization.di.localizationModule
import com.phamtunglam.lamity.feature.settings.di.settingsModule
import com.phamtunglam.lamity.feature.theme.di.themeModule
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Application-wide infrastructure.
 */
val appModule: Module =
    module {
        includes(
            coreModule,
            chatModule,
            localizationModule,
            modelsModule,
            settingsModule,
            themeModule,
            dbModule,
            llmModule,
            platformModule(),
        )
    }
