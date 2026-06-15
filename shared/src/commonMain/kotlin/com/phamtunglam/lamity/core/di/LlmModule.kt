package com.phamtunglam.lamity.core.di

import com.phamtunglam.lamity.core.domain.tools.AppTool
import com.phamtunglam.lamity.core.domain.tools.CalculateTool
import com.phamtunglam.lamity.core.domain.tools.DeviceInfoTool
import com.phamtunglam.lamity.core.domain.tools.GetCurrentTimeTool
import com.phamtunglam.lamity.core.domain.tools.RandomNumberTool
import com.phamtunglam.lamity.core.domain.tools.SetLanguageTool
import com.phamtunglam.lamity.core.domain.tools.SetThemeTool
import com.phamtunglam.lamity.feature.chat.domain.ModelRuntime
import org.koin.core.module.Module
import org.koin.dsl.module

val llmModule: Module =
    module {
        // The user-selectable built-in tools, shared by the chat session and the Studio UI.
        // load_skill is created per chat session (it needs that session's skills), not listed here.
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
        single { ModelRuntime() }
    }
