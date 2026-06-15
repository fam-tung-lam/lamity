package com.phamtunglam.lamity.core.di

import com.phamtunglam.lamity.feature.chat.di.chatModule
import com.phamtunglam.lamity.feature.history.di.historyModule
import com.phamtunglam.lamity.feature.localization.di.localizationModule
import com.phamtunglam.lamity.feature.models.di.modelsModule
import com.phamtunglam.lamity.feature.settings.di.settingsModule
import com.phamtunglam.lamity.feature.studio.di.studioModule
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
            historyModule,
            localizationModule,
            modelsModule,
            settingsModule,
            studioModule,
            dbModule,
            llmModule,
            platformModule(),
        )
    }
