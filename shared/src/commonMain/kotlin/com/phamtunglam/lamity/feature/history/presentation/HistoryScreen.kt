package com.phamtunglam.lamity.feature.history.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phamtunglam.lamity.core.presentation.designSystem.components.ConfirmDialog
import com.phamtunglam.lamity.core.presentation.designSystem.components.EmptyState
import com.phamtunglam.lamity.core.presentation.i18n.LocalStrings
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HistoryScreen(
    onOpenChat: () -> Unit,
    viewModel: HistoryViewModel = koinViewModel(),
) {
    val str = LocalStrings.current
    val ui by viewModel.uiState.collectAsState()

    if (ui.rows.isEmpty()) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            EmptyState(str.historyEmptyTitle, str.historyEmptyBody)
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(ui.rows, key = { it.conversation.id }) { row ->
            ConversationCard(
                row = row,
                onOpen = {
                    viewModel.open(row.conversation.id)
                    onOpenChat()
                },
                onRename = { viewModel.rename(row.conversation.id, it) },
                onDelete = { viewModel.delete(row.conversation.id) },
            )
        }
    }
}

@Composable
private fun ConversationCard(
    row: ConversationRowUiState,
    onOpen: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val str = LocalStrings.current
    var menuOpen by remember { mutableStateOf(false) }
    var renameOpen by remember { mutableStateOf(false) }
    var deleteOpen by remember { mutableStateOf(false) }

    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    row.conversation.title.ifBlank { str.newChat },
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    "${row.agentName ?: str.noAgent} • ${row.modelName} • ${row.updatedAtText}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = null)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(str.rename) },
                        onClick = { menuOpen = false; renameOpen = true },
                    )
                    DropdownMenuItem(
                        text = { Text(str.delete, color = MaterialTheme.colorScheme.error) },
                        onClick = { menuOpen = false; deleteOpen = true },
                    )
                }
            }
        }
    }

    if (renameOpen) {
        var title by remember { mutableStateOf(row.conversation.title) }
        AlertDialog(
            onDismissRequest = { renameOpen = false },
            title = { Text(str.renameConversation) },
            text = {
                OutlinedTextField(value = title, onValueChange = { title = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    onRename(title)
                    renameOpen = false
                }) { Text(str.save) }
            },
            dismissButton = {
                TextButton(onClick = { renameOpen = false }) { Text(str.cancel) }
            },
        )
    }

    if (deleteOpen) {
        ConfirmDialog(
            title = str.confirmDeleteTitle,
            text = str.deleteConversationQ,
            confirmLabel = str.delete,
            onConfirm = onDelete,
            onDismiss = { deleteOpen = false },
        )
    }
}
