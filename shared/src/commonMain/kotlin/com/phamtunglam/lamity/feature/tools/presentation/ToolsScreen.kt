package com.phamtunglam.lamity.feature.tools.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phamtunglam.lamity.core.presentation.designSystem.components.SubScreenScaffold
import com.phamtunglam.lamity.shared.resources.Res
import com.phamtunglam.lamity.shared.resources.tools_caption
import com.phamtunglam.lamity.shared.resources.tools_tab
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ToolsScreen(onBack: () -> Unit, viewModel: ToolsViewModel = koinViewModel()) {
    val tools = viewModel.uiState.tools

    SubScreenScaffold(title = stringResource(Res.string.tools_tab), onBack = onBack) {
        Column(Modifier.fillMaxSize()) {
            Caption(stringResource(Res.string.tools_caption))
            LazyColumn(
                contentPadding = PaddingValues(12.dp, 0.dp, 12.dp, 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(tools, key = { it.id }) { tool ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text(tool.displayName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                tool.id,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                tool.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Caption(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
