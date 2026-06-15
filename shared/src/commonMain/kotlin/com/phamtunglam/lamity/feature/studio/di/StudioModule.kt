package com.phamtunglam.lamity.feature.studio.di

import com.phamtunglam.lamity.feature.studio.data.AgentsRepository
import com.phamtunglam.lamity.feature.studio.data.AgentsRepositoryImpl
import com.phamtunglam.lamity.feature.studio.data.SkillsRepository
import com.phamtunglam.lamity.feature.studio.data.SkillsRepositoryImpl
import com.phamtunglam.lamity.feature.studio.domain.DeleteAgentUseCase
import com.phamtunglam.lamity.feature.studio.domain.DeleteSkillUseCase
import com.phamtunglam.lamity.feature.studio.domain.SaveAgentUseCase
import com.phamtunglam.lamity.feature.studio.domain.SaveSkillUseCase
import com.phamtunglam.lamity.feature.studio.presentation.AgentEditViewModel
import com.phamtunglam.lamity.feature.studio.presentation.SkillEditViewModel
import com.phamtunglam.lamity.feature.studio.presentation.StudioViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val studioModule: Module =
    module {
        // Data
        single<AgentsRepository> { AgentsRepositoryImpl(get(), get()) }
        single<SkillsRepository> { SkillsRepositoryImpl(get(), get()) }

        // Domain
        factory { DeleteAgentUseCase(get(), get()) }
        factory { DeleteSkillUseCase(get(), get()) }
        factory { SaveAgentUseCase(get()) }
        factory { SaveSkillUseCase(get()) }

        // Presentation
        viewModel { StudioViewModel(get(), get(), get(), get(), get(), get()) }
        viewModel { params ->
            AgentEditViewModel(params.getOrNull<String>(), get(), get(), get(), get())
        }
        viewModel { params -> SkillEditViewModel(params.getOrNull<String>(), get(), get()) }
    }
