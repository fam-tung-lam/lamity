package com.phamtunglam.lamity

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.phamtunglam.lamity.core.presentation.designSystem.theme.AppTheme
import com.phamtunglam.lamity.feature.localization.presentation.AppLocaleEnvironment
import com.phamtunglam.lamity.feature.localization.presentation.LocalizationViewModel
import com.phamtunglam.lamity.feature.chat.presentation.ChatScreen
import com.phamtunglam.lamity.feature.history.presentation.HistoryScreen
import com.phamtunglam.lamity.feature.models.presentation.ModelConfigScreen
import com.phamtunglam.lamity.feature.models.presentation.ModelsScreen
import com.phamtunglam.lamity.feature.settings.data.SettingsRepository
import com.phamtunglam.lamity.feature.settings.presentation.SettingsScreen
import com.phamtunglam.lamity.feature.studio.presentation.AgentEditScreen
import com.phamtunglam.lamity.feature.studio.presentation.SkillEditScreen
import com.phamtunglam.lamity.feature.studio.presentation.StudioScreen
import com.phamtunglam.lamity.core.presentation.navigation.AgentEditKey
import com.phamtunglam.lamity.core.presentation.navigation.ChatKey
import com.phamtunglam.lamity.core.presentation.navigation.HistoryKey
import com.phamtunglam.lamity.core.presentation.navigation.ModelConfigKey
import com.phamtunglam.lamity.core.presentation.navigation.ModelsKey
import com.phamtunglam.lamity.core.presentation.navigation.SettingsKey
import com.phamtunglam.lamity.core.presentation.navigation.SkillEditKey
import com.phamtunglam.lamity.core.presentation.navigation.StudioKey
import com.phamtunglam.lamity.core.presentation.navigation.TabKey
import com.phamtunglam.lamity.core.presentation.navigation.navSavedStateConfiguration
import com.phamtunglam.lamity.shared.resources.Res
import com.phamtunglam.lamity.shared.resources.tab_chat
import com.phamtunglam.lamity.shared.resources.tab_history
import com.phamtunglam.lamity.shared.resources.tab_models
import com.phamtunglam.lamity.shared.resources.tab_settings
import com.phamtunglam.lamity.shared.resources.tab_studio
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    val settingsRepository = koinInject<SettingsRepository>()
    val settings by settingsRepository.settings.collectAsState()
    val localeViewModel = koinViewModel<LocalizationViewModel>()
    val localeState by localeViewModel.state.collectAsState()

    AppLocaleEnvironment(locale = localeState.current) {
        AppTheme(settings.themeMode) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                val backStack = rememberNavBackStack(navSavedStateConfiguration, ChatKey)

                fun switchTab(tab: TabKey) {
                    backStack.clear()
                    backStack.add(tab)
                }

                Scaffold(
                    bottomBar = {
                        if (backStack.size == 1) {
                            NavigationBar {
                                val current = backStack.firstOrNull()
                                TabItem(current, ChatKey, stringResource(Res.string.tab_chat), Icons.AutoMirrored.Filled.Send, ::switchTab)
                                TabItem(current, ModelsKey, stringResource(Res.string.tab_models), Icons.Default.Home, ::switchTab)
                                TabItem(current, HistoryKey, stringResource(Res.string.tab_history), Icons.Default.DateRange, ::switchTab)
                                TabItem(current, StudioKey, stringResource(Res.string.tab_studio), Icons.Default.Build, ::switchTab)
                                TabItem(current, SettingsKey, stringResource(Res.string.tab_settings), Icons.Default.Settings, ::switchTab)
                            }
                        }
                    },
                ) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding)) {
                        AppNavDisplay(
                            backStack = backStack,
                            onSwitchTab = ::switchTab,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppNavDisplay(
    backStack: NavBackStack<NavKey>,
    onSwitchTab: (TabKey) -> Unit,
) {
    fun pop() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    NavDisplay(
        backStack = backStack,
        onBack = { pop() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<ChatKey> {
                ChatScreen(onGoToModels = { onSwitchTab(ModelsKey) })
            }
            entry<ModelsKey> {
                ModelsScreen(
                    onOpenChat = { onSwitchTab(ChatKey) },
                    onConfigureModel = { modelId -> backStack.add(ModelConfigKey(modelId)) },
                )
            }
            entry<HistoryKey> {
                HistoryScreen(onOpenChat = { onSwitchTab(ChatKey) })
            }
            entry<StudioKey> {
                StudioScreen(
                    onEditAgent = { agentId -> backStack.add(AgentEditKey(agentId)) },
                    onEditSkill = { skillId -> backStack.add(SkillEditKey(skillId)) },
                )
            }
            entry<SettingsKey> {
                SettingsScreen()
            }
            entry<AgentEditKey> { key ->
                AgentEditScreen(agentId = key.agentId, onBack = ::pop)
            }
            entry<SkillEditKey> { key ->
                SkillEditScreen(skillId = key.skillId, onBack = ::pop)
            }
            entry<ModelConfigKey> { key ->
                ModelConfigScreen(modelId = key.modelId, onBack = ::pop)
            }
        },
    )
}

@Composable
private fun RowScope.TabItem(
    current: NavKey?,
    tab: TabKey,
    label: String,
    icon: ImageVector,
    onSelect: (TabKey) -> Unit,
) {
    NavigationBarItem(
        selected = current == tab,
        onClick = { onSelect(tab) },
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label) },
    )
}
