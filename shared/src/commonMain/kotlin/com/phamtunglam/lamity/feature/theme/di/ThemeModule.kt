package com.phamtunglam.lamity.feature.theme.di

import com.phamtunglam.lamity.feature.theme.data.ThemeRepository
import com.phamtunglam.lamity.feature.theme.data.ThemeRepositoryImpl
import com.phamtunglam.lamity.feature.theme.domain.ObserveThemeUseCase
import com.phamtunglam.lamity.feature.theme.domain.SetThemeUseCase
import com.phamtunglam.lamity.feature.theme.presentation.ThemeViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val themeModule: Module =
    module {
        // Data
        single<ThemeRepository> { ThemeRepositoryImpl(get()) }

        // Domain
        factory { ObserveThemeUseCase(get()) }
        factory { SetThemeUseCase(get()) }

        // Presentation
        viewModel { ThemeViewModel(get(), get()) }
    }
