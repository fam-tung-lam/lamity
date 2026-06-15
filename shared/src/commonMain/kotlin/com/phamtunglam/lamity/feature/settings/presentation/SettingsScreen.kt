package com.phamtunglam.lamity.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phamtunglam.lamity.core.LamityBuildConfig
import com.phamtunglam.lamity.core.presentation.designSystem.components.SimpleDropdown
import com.phamtunglam.lamity.core.presentation.designSystem.components.SubScreenScaffold
import com.phamtunglam.lamity.feature.localization.domain.AppLocale
import com.phamtunglam.lamity.feature.localization.presentation.LocalizationViewModel
import com.phamtunglam.lamity.feature.settings.domain.ThemeMode
import com.phamtunglam.lamity.shared.resources.Res
import com.phamtunglam.lamity.shared.resources.about
import com.phamtunglam.lamity.shared.resources.downloads_section
import com.phamtunglam.lamity.shared.resources.language
import com.phamtunglam.lamity.shared.resources.language_system
import com.phamtunglam.lamity.shared.resources.settings_title
import com.phamtunglam.lamity.shared.resources.theme
import com.phamtunglam.lamity.shared.resources.theme_dark
import com.phamtunglam.lamity.shared.resources.theme_light
import com.phamtunglam.lamity.shared.resources.theme_system
import com.phamtunglam.lamity.shared.resources.wifi_only
import com.phamtunglam.lamity.shared.resources.wifi_only_hint
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
    localeViewModel: LocalizationViewModel = koinViewModel(),
) {
    val ui by viewModel.uiState.collectAsState()
    val settings = ui.settings
    val localeState by localeViewModel.state.collectAsState()

    SubScreenScaffold(title = stringResource(Res.string.settings_title), onBack = onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Theme (also switchable by the set_theme tool)
            ThemeSection(selected = settings.themeMode, onSelect = viewModel::setThemeMode)

            // Language (also switchable by the set_language tool)
            LanguageSection(currentBcp47 = localeState.current?.bcp47, onSelect = localeViewModel::onLocaleSelected)

            DownloadsSection(wifiOnly = settings.wifiOnlyDownloads, onWifiOnlyChange = viewModel::setWifiOnlyDownloads)

            AboutSection(
                platform = ui.platformInfo.platform,
                osVersion = ui.platformInfo.osVersion,
                deviceModel = ui.platformInfo.deviceModel,
                modelsDir = ui.modelsDir,
            )
        }
    }
}

@Composable
private fun ThemeSection(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    Column {
        Text(stringResource(Res.string.theme), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.padding(3.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            val modes =
                listOf(
                    ThemeMode.LIGHT to stringResource(Res.string.theme_light),
                    ThemeMode.DARK to stringResource(Res.string.theme_dark),
                    ThemeMode.SYSTEM to stringResource(Res.string.theme_system),
                )
            modes.forEachIndexed { index, (mode, label) ->
                SegmentedButton(
                    selected = selected == mode,
                    onClick = { onSelect(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                ) { Text(label) }
            }
        }
    }
}

@Composable
private fun LanguageSection(currentBcp47: String?, onSelect: (AppLocale?) -> Unit) {
    Column {
        val languageLabel = stringResource(Res.string.language)
        Text(languageLabel, style = MaterialTheme.typography.titleSmall)
        val options =
            buildList {
                add(null to stringResource(Res.string.language_system))
                AppLocale.entries.forEach { locale -> add(locale.bcp47 to locale.displayName) }
            }
        SimpleDropdown(
            label = languageLabel,
            options = options,
            selectedId = currentBcp47,
            onSelect = { tag ->
                onSelect(tag?.let { bcp47 -> AppLocale.entries.firstOrNull { it.bcp47 == bcp47 } })
            },
        )
    }
}

@Composable
private fun DownloadsSection(wifiOnly: Boolean, onWifiOnlyChange: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(Res.string.downloads_section), style = MaterialTheme.typography.titleSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.wifi_only), style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(Res.string.wifi_only_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = wifiOnly,
                onCheckedChange = onWifiOnlyChange,
            )
        }
    }
}

@Composable
private fun AboutSection(
    platform: String,
    osVersion: String,
    deviceModel: String,
    modelsDir: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.about), style = MaterialTheme.typography.titleSmall)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Lamity AI ${LamityBuildConfig.appVersion} (${LamityBuildConfig.appVersionCode})",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    "$platform $osVersion • $deviceModel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "On-device inference by Google LiteRT-LM. Models from the HuggingFace litert-community.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Models dir: $modelsDir",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}
