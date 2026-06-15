package com.phamtunglam.lamity.feature.skills.di

import com.phamtunglam.lamity.feature.skills.data.SkillsRepository
import com.phamtunglam.lamity.feature.skills.data.SkillsRepositoryImpl
import com.phamtunglam.lamity.feature.skills.domain.DeleteSkillUseCase
import com.phamtunglam.lamity.feature.skills.domain.SaveSkillUseCase
import com.phamtunglam.lamity.feature.skills.presentation.SkillEditViewModel
import com.phamtunglam.lamity.feature.skills.presentation.SkillsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val skillsModule: Module =
    module {
        // Data
        single<SkillsRepository> { SkillsRepositoryImpl(get(), get()) }

        // Domain
        factory { DeleteSkillUseCase(get(), get()) }
        factory { SaveSkillUseCase(get()) }

        // Presentation
        viewModel { SkillsViewModel(get(), get()) }
        viewModel { params -> SkillEditViewModel(params.getOrNull<String>(), get(), get()) }
    }
