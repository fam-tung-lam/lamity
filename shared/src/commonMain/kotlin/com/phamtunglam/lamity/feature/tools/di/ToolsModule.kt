package com.phamtunglam.lamity.feature.tools.di

import com.phamtunglam.lamity.feature.tools.domain.AppTool
import com.phamtunglam.lamity.feature.tools.domain.CalculateTool
import com.phamtunglam.lamity.feature.tools.domain.DeviceInfoTool
import com.phamtunglam.lamity.feature.tools.domain.GetCurrentTimeTool
import com.phamtunglam.lamity.feature.tools.domain.RandomNumberTool
import com.phamtunglam.lamity.feature.tools.domain.SetLanguageTool
import com.phamtunglam.lamity.feature.tools.domain.SetThemeTool
import org.koin.core.module.Module
import org.koin.dsl.module

val toolsModule: Module =
    module {
        // The built-in tools, shared by the chat session and its settings sheet. load_skill is
        // created per chat session (it needs that session's skills), not listed here.
        single<List<AppTool>> {
            listOf(
                GetCurrentTimeTool(),
                CalculateTool(),
                SetThemeTool(get()),
                SetLanguageTool(get()),
                RandomNumberTool(),
                DeviceInfoTool(get()),
            )
        }
    }
