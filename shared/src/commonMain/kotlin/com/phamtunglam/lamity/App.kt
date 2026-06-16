package com.phamtunglam.lamity

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.phamtunglam.lamity.core.presentation.designSystem.theme.AppTheme
import com.phamtunglam.lamity.core.presentation.navigation.ChatKey
import com.phamtunglam.lamity.core.presentation.navigation.ChatsKey
import com.phamtunglam.lamity.core.presentation.navigation.ModelsKey
import com.phamtunglam.lamity.core.presentation.navigation.SettingsKey
import com.phamtunglam.lamity.core.presentation.navigation.navSavedStateConfiguration
import com.phamtunglam.lamity.feature.chat.presentation.ChatScreen
import com.phamtunglam.lamity.feature.chat.presentation.ChatsScreen
import com.phamtunglam.lamity.feature.llmModels.presentation.ModelsScreen
import com.phamtunglam.lamity.feature.localization.presentation.AppLocaleEnvironment
import com.phamtunglam.lamity.feature.localization.presentation.LocalizationViewModel
import com.phamtunglam.lamity.feature.settings.presentation.SettingsScreen
import com.phamtunglam.lamity.feature.theme.presentation.ThemeViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    val localeViewModel = koinViewModel<LocalizationViewModel>()
    val localeState by localeViewModel.state.collectAsState()
    val themeViewModel = koinViewModel<ThemeViewModel>()
    val themeState by themeViewModel.state.collectAsState()

    val focusManager = LocalFocusManager.current

    AppLocaleEnvironment(locale = localeState.current) {
        AppTheme(themeState.current) {
            Surface(
                modifier =
                    Modifier
                        .fillMaxSize()
                        // Tapping outside any focused input (empty/non-interactive areas) dismisses the
                        // keyboard. Clickable children consume the tap first, so this only fires on blank space.
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { focusManager.clearFocus() })
                        },
                color = MaterialTheme.colorScheme.background,
            ) {
                val backStack = rememberNavBackStack(navSavedStateConfiguration, ChatsKey)
                AppNavDisplay(backStack)
            }
        }
    }
}

@Composable
private fun AppNavDisplay(backStack: NavBackStack<NavKey>) {
    fun pop() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    fun open(key: NavKey) = backStack.add(key)

    NavDisplay(
        backStack = backStack,
        onBack = { pop() },
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider =
            entryProvider {
                entry<ChatsKey> {
                    ChatsScreen(
                        onOpenChat = { conversationId -> open(ChatKey(conversationId)) },
                        onOpenSettings = { open(SettingsKey) },
                    )
                }
                entry<ChatKey> { key ->
                    ChatScreen(
                        conversationId = key.conversationId,
                        onBack = ::pop,
                        onOpenModels = { open(ModelsKey) },
                    )
                }
                entry<ModelsKey> {
                    ModelsScreen(onModelSelected = ::pop, onBack = ::pop)
                }
                entry<SettingsKey> {
                    SettingsScreen(onBack = ::pop, onOpenModels = { open(ModelsKey) })
                }
            },
    )
}
