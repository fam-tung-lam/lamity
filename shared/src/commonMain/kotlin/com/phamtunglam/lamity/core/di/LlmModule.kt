package com.phamtunglam.lamity.core.di

import com.phamtunglam.lamity.feature.chat.domain.ModelRuntime
import org.koin.core.module.Module
import org.koin.dsl.module

val llmModule: Module =
    module {
        single { ModelRuntime() }
    }
