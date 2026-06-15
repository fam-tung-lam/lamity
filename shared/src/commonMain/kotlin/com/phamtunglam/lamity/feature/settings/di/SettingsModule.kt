package com.phamtunglam.lamity.feature.settings.di

import com.phamtunglam.lamity.feature.settings.data.SettingsRepository
import com.phamtunglam.lamity.feature.settings.data.SettingsRepositoryImpl
import com.phamtunglam.lamity.feature.settings.presentation.SettingsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsModule: Module =
    module {
        // Data
        single<SettingsRepository> { SettingsRepositoryImpl(get(), get()) }

        // Presentation
        viewModel { SettingsViewModel(get(), get(), get()) }
    }
