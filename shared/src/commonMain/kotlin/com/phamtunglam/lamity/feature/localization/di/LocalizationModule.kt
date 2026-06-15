package com.phamtunglam.lamity.feature.localization.di

import com.phamtunglam.lamity.feature.localization.data.AppLocaleRepository
import com.phamtunglam.lamity.feature.localization.data.AppLocaleStore
import com.phamtunglam.lamity.feature.localization.data.DataStoreAppLocaleStore
import com.phamtunglam.lamity.feature.localization.data.DeviceLocaleProvider
import com.phamtunglam.lamity.feature.localization.data.systemDeviceLocaleProvider
import com.phamtunglam.lamity.feature.localization.presentation.LocalizationViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val localizationModule: Module =
    module {
        // Data
        single<AppLocaleStore> { DataStoreAppLocaleStore(get()) }
        single<DeviceLocaleProvider> { systemDeviceLocaleProvider() }
        single { AppLocaleRepository(get(), get()) }

        // Presentation
        viewModel { LocalizationViewModel(get(), get()) }
    }
