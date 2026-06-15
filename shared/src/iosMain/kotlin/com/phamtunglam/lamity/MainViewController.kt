package com.phamtunglam.lamity

import androidx.compose.ui.window.ComposeUIViewController
import com.phamtunglam.lamity.core.di.appModule
import com.phamtunglam.lamity.core.di.platformModule
import org.koin.core.context.startKoin
import platform.UIKit.UIViewController

private var koinStarted = false

/**
 * iOS entry point: starts Koin and returns the Compose root view controller.
 *
 * See iosApp/ContentView.swift.
 */
@Suppress("FunctionName", "Unused") // Swift calls this iOS entry point by name.
fun MainViewController(): UIViewController {
    if (!koinStarted) {
        startKoin {
            modules(platformModule(), appModule)
        }
        koinStarted = true
    }
    return ComposeUIViewController { App() }
}
