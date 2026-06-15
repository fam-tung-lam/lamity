package com.phamtunglam.lamity.feature.models.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
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
import com.phamtunglam.lamity.core.presentation.designSystem.components.SubScreenScaffold
import com.phamtunglam.lamity.feature.models.presentation.components.AddCustomModelDialog
import com.phamtunglam.lamity.feature.models.presentation.components.ModelCard
import com.phamtunglam.lamity.shared.resources.Res
import com.phamtunglam.lamity.shared.resources.add_custom_model
import com.phamtunglam.lamity.shared.resources.models_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ModelsScreen(
    onOpenChat: () -> Unit,
    onConfigureModel: (modelId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: ModelsViewModel = koinViewModel(),
) {
    val ui by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    SubScreenScaffold(title = stringResource(Res.string.models_title), onBack = onBack) {
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                contentPadding = PaddingValues(12.dp, 12.dp, 12.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(ui.rows, key = { it.model.id }) { row ->
                    ModelCard(
                        row = row,
                        viewModel = viewModel,
                        onOpenChat = onOpenChat,
                        onConfigureModel = onConfigureModel,
                    )
                }
            }
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(Res.string.add_custom_model)) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            )
        }
    }

    if (showAddDialog) {
        AddCustomModelDialog(
            onAdd = { name, url, requiresAuth -> viewModel.addCustomModel(name, url, requiresAuth) },
            onDismiss = { showAddDialog = false },
        )
    }
}
