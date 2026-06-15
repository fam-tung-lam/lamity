package com.phamtunglam.lamity.core.di

import com.phamtunglam.lamity.feature.agents.di.agentsModule
import com.phamtunglam.lamity.feature.chat.di.chatModule
import com.phamtunglam.lamity.feature.history.di.historyModule
import com.phamtunglam.lamity.feature.home.di.homeModule
import com.phamtunglam.lamity.feature.localization.di.localizationModule
import com.phamtunglam.lamity.feature.models.di.modelsModule
import com.phamtunglam.lamity.feature.settings.di.settingsModule
import com.phamtunglam.lamity.feature.skills.di.skillsModule
import com.phamtunglam.lamity.feature.tools.di.toolsModule
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Application-wide infrastructure.
 */
val appModule: Module =
    module {
        includes(
            coreModule,
            agentsModule,
            chatModule,
            historyModule,
            homeModule,
            localizationModule,
            modelsModule,
            settingsModule,
            skillsModule,
            toolsModule,
            dbModule,
            llmModule,
            platformModule(),
        )
    }
