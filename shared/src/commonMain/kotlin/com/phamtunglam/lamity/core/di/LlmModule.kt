package com.phamtunglam.lamity.core.di

import com.phamtunglam.lamity.core.domain.tools.ToolContext
import com.phamtunglam.lamity.core.domain.tools.ToolDispatcher
import com.phamtunglam.lamity.core.domain.tools.ToolRegistry
import com.phamtunglam.lamity.llm.ModelRuntime
import org.koin.core.module.Module
import org.koin.dsl.module

val llmModule: Module = module {
    single { ToolContext(get(), get(), get()) }
    single { ToolRegistry(get()) }
    single { ToolDispatcher(get()) }
    single { ModelRuntime(get()) }
}
