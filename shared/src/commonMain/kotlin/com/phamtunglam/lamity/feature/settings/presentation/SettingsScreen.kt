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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phamtunglam.lamity.core.platform.currentBuildInfo
import com.phamtunglam.lamity.core.designsystem.components.SimpleDropdown
import com.phamtunglam.lamity.core.i18n.LocalStrings
import com.phamtunglam.lamity.feature.settings.domain.ThemeMode
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val str = LocalStrings.current
    val ui by viewModel.uiState.collectAsState()
    val settings = ui.settings

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Theme (also switchable by the set_theme tool)
        Column {
            Text(str.theme, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.padding(3.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val modes = listOf(
                    ThemeMode.LIGHT to str.themeLight,
                    ThemeMode.DARK to str.themeDark,
                    ThemeMode.SYSTEM to str.themeSystem,
                )
                modes.forEachIndexed { index, (mode, label) ->
                    SegmentedButton(
                        selected = settings.themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                    ) { Text(label) }
                }
            }
        }

        // Language (also switchable by the set_language tool)
        Column {
            Text(str.language, style = MaterialTheme.typography.titleSmall)
            SimpleDropdown(
                label = str.language,
                options = listOf<Pair<String?, String>>(
                    "en" to "English",
                    "vi" to "Tiếng Việt",
                    "es" to "Español",
                ),
                selectedId = settings.language,
                onSelect = { it?.let(viewModel::setLanguage) },
            )
        }

        // Downloads
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(str.downloadsSection, style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(str.wifiOnly, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        str.wifiOnlyHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = settings.wifiOnlyDownloads,
                    onCheckedChange = viewModel::setWifiOnlyDownloads,
                )
            }
        }

        // HuggingFace token for gated models
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(str.hfToken, style = MaterialTheme.typography.titleSmall)
            var token by remember(settings.hfToken) { mutableStateOf(settings.hfToken) }
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                supportingText = { Text(str.hfTokenHint) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row {
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { viewModel.setHfToken(token) },
                    enabled = token.trim() != settings.hfToken,
                ) { Text(str.save) }
            }
        }

        // About
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(str.about, style = MaterialTheme.typography.titleSmall)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val build = remember { currentBuildInfo() }
                    Text(
                        "Lamity AI ${build.versionName} (${build.versionCode})",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        "${ui.platformInfo.platform} ${ui.platformInfo.osVersion} • " +
                            ui.platformInfo.deviceModel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "On-device inference by Google LiteRT-LM. Models from the HuggingFace litert-community.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Models dir: ${ui.modelsDir}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}
