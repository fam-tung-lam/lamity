package com.phamtunglam.lamity

import androidx.compose.ui.window.ComposeUIViewController
import com.phamtunglam.lamity.core.di.appModule
import com.phamtunglam.lamity.core.di.platformModule
import com.phamtunglam.lamity.core.domain.tools.ToolDispatcher
import com.phamtunglam.lamity.llm.NativeLlmBridge
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import platform.UIKit.UIViewController

private var koinStarted = false

/**
 * iOS entry point. The Swift side passes its LiteRT-LM bridge implementation
 * and receives the Compose root view controller (see iosApp/ContentView.swift).
 */
@Suppress("FunctionName", "ktlint:standard:function-naming") // Swift calls this iOS entry point by name.
fun MainViewController(llmBridge: NativeLlmBridge): UIViewController {
    if (!koinStarted) {
        startKoin {
            modules(
                platformModule(),
                module {
                    single<NativeLlmBridge> { llmBridge }
                },
                appModule,
            )
        }
        koinStarted = true
    }
    return ComposeUIViewController { App() }
}

/** Tool execution entry for the Swift tool structs. */
fun executeBuiltinTool(toolId: String, paramsJson: String): String =
    if (koinStarted) {
        KoinPlatform.getKoin().get<ToolDispatcher>().executeTool(toolId, paramsJson)
    } else {
        """{"error":"app container not ready"}"""
    }
