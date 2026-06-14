package com.phamtunglam.lamity.di

import com.phamtunglam.lamity.core.tools.ToolContext
import com.phamtunglam.lamity.core.tools.ToolDispatcher
import com.phamtunglam.lamity.core.tools.ToolRegistry
import com.phamtunglam.lamity.llm.ModelRuntime
import org.koin.core.module.Module
import org.koin.dsl.module

val llmModule: Module = module {
    single { ToolContext(get(), get(), get()) }
    single { ToolRegistry(get()) }
    single { ToolDispatcher(get()) }
    single { ModelRuntime(get()) }
}
