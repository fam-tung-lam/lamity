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
import com.phamtunglam.lamity.core.presentation.navigation.AgentEditKey
import com.phamtunglam.lamity.core.presentation.navigation.AgentsKey
import com.phamtunglam.lamity.core.presentation.navigation.ChatKey
import com.phamtunglam.lamity.core.presentation.navigation.ChatsKey
import com.phamtunglam.lamity.core.presentation.navigation.HomeKey
import com.phamtunglam.lamity.core.presentation.navigation.ModelsKey
import com.phamtunglam.lamity.core.presentation.navigation.SettingsKey
import com.phamtunglam.lamity.core.presentation.navigation.SkillEditKey
import com.phamtunglam.lamity.core.presentation.navigation.SkillsKey
import com.phamtunglam.lamity.core.presentation.navigation.ToolsKey
import com.phamtunglam.lamity.core.presentation.navigation.navSavedStateConfiguration
import com.phamtunglam.lamity.feature.agents.presentation.AgentEditScreen
import com.phamtunglam.lamity.feature.agents.presentation.AgentsScreen
import com.phamtunglam.lamity.feature.chat.presentation.ChatScreen
import com.phamtunglam.lamity.feature.history.presentation.ChatsScreen
import com.phamtunglam.lamity.feature.home.presentation.HomeScreen
import com.phamtunglam.lamity.feature.localization.presentation.AppLocaleEnvironment
import com.phamtunglam.lamity.feature.localization.presentation.LocalizationViewModel
import com.phamtunglam.lamity.feature.models.presentation.ModelsScreen
import com.phamtunglam.lamity.feature.settings.data.SettingsRepository
import com.phamtunglam.lamity.feature.settings.presentation.SettingsScreen
import com.phamtunglam.lamity.feature.skills.presentation.SkillEditScreen
import com.phamtunglam.lamity.feature.skills.presentation.SkillsScreen
import com.phamtunglam.lamity.feature.tools.presentation.ToolsScreen
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    val settingsRepository = koinInject<SettingsRepository>()
    val settings by settingsRepository.settings.collectAsState()
    val localeViewModel = koinViewModel<LocalizationViewModel>()
    val localeState by localeViewModel.state.collectAsState()

    val focusManager = LocalFocusManager.current

    AppLocaleEnvironment(locale = localeState.current) {
        AppTheme(settings.themeMode) {
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
                val backStack = rememberNavBackStack(navSavedStateConfiguration, HomeKey)
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
                entry<HomeKey> {
                    HomeScreen(
                        onOpenAgents = { open(AgentsKey) },
                        onOpenSkills = { open(SkillsKey) },
                        onOpenTools = { open(ToolsKey) },
                        onOpenChats = { open(ChatsKey) },
                        onOpenModels = { open(ModelsKey) },
                        onOpenSettings = { open(SettingsKey) },
                    )
                }
                entry<ChatsKey> {
                    ChatsScreen(onOpenChat = { conversationId -> open(ChatKey(conversationId)) }, onBack = ::pop)
                }
                entry<ChatKey> { key ->
                    ChatScreen(
                        conversationId = key.conversationId,
                        onBack = ::pop,
                        onGoToModels = { open(ModelsKey) },
                    )
                }
                entry<AgentsKey> {
                    AgentsScreen(onEditAgent = { agentId -> open(AgentEditKey(agentId)) }, onBack = ::pop)
                }
                entry<SkillsKey> {
                    SkillsScreen(onEditSkill = { skillId -> open(SkillEditKey(skillId)) }, onBack = ::pop)
                }
                entry<ToolsKey> {
                    ToolsScreen(onBack = ::pop)
                }
                entry<ModelsKey> {
                    ModelsScreen(
                        onOpenChat = { open(ChatKey()) },
                        onBack = ::pop,
                    )
                }
                entry<SettingsKey> {
                    SettingsScreen(onBack = ::pop)
                }
                entry<AgentEditKey> { key ->
                    AgentEditScreen(agentId = key.agentId, onBack = ::pop)
                }
                entry<SkillEditKey> { key ->
                    SkillEditScreen(skillId = key.skillId, onBack = ::pop)
                }
            },
    )
}
