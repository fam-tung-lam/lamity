package com.phamtunglam.lamity.feature.tools.di

import com.phamtunglam.lamity.feature.tools.domain.AppTool
import com.phamtunglam.lamity.feature.tools.domain.CalculateTool
import com.phamtunglam.lamity.feature.tools.domain.DeviceInfoTool
import com.phamtunglam.lamity.feature.tools.domain.GetCurrentTimeTool
import com.phamtunglam.lamity.feature.tools.domain.RandomNumberTool
import com.phamtunglam.lamity.feature.tools.domain.SetLanguageTool
import com.phamtunglam.lamity.feature.tools.domain.SetThemeTool
import com.phamtunglam.lamity.feature.tools.presentation.ToolsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val toolsModule: Module =
    module {
        // The user-selectable built-in tools, shared by the chat session and the Tools UI.
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

        // Presentation
        viewModel { ToolsViewModel(get(), get()) }
    }
