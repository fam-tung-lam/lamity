package com.phamtunglam.lamity.feature.home.di

import com.phamtunglam.lamity.feature.home.presentation.HomeViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val homeModule: Module =
    module {
        viewModel { HomeViewModel(get(), get(), get(), get(), get()) }
    }
