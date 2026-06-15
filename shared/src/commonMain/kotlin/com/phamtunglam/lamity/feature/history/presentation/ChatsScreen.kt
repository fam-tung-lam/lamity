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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
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
import com.phamtunglam.lamity.core.presentation.designSystem.components.SubScreenScaffold
import com.phamtunglam.lamity.shared.resources.Res
import com.phamtunglam.lamity.shared.resources.cancel
import com.phamtunglam.lamity.shared.resources.chats_title
import com.phamtunglam.lamity.shared.resources.confirm_delete_title
import com.phamtunglam.lamity.shared.resources.delete
import com.phamtunglam.lamity.shared.resources.delete_conversation_q
import com.phamtunglam.lamity.shared.resources.history_empty_body
import com.phamtunglam.lamity.shared.resources.history_empty_title
import com.phamtunglam.lamity.shared.resources.new_chat
import com.phamtunglam.lamity.shared.resources.rename
import com.phamtunglam.lamity.shared.resources.rename_conversation
import com.phamtunglam.lamity.shared.resources.save
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChatsScreen(onOpenChat: (String?) -> Unit, onBack: () -> Unit, viewModel: HistoryViewModel = koinViewModel()) {
    val ui by viewModel.uiState.collectAsState()

    SubScreenScaffold(title = stringResource(Res.string.chats_title), onBack = onBack) {
        Box(Modifier.fillMaxSize()) {
            if (ui.rows.isEmpty()) {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    EmptyState(
                        stringResource(Res.string.history_empty_title),
                        stringResource(Res.string.history_empty_body),
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp, 12.dp, 12.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(ui.rows, key = { it.conversation.id }) { row ->
                        ConversationCard(
                            row = row,
                            onOpen = { onOpenChat(row.conversation.id) },
                            onRename = { viewModel.rename(row.conversation.id, it) },
                            onDelete = { viewModel.delete(row.conversation.id) },
                        )
                    }
                }
            }
            ExtendedFloatingActionButton(
                onClick = { onOpenChat(null) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(Res.string.new_chat)) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
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
                    row.conversation.title.ifBlank { stringResource(Res.string.new_chat) },
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    row.updatedAtText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ConversationCardMenu(
                onRename = { renameOpen = true },
                onDelete = { deleteOpen = true },
            )
        }
    }

    if (renameOpen) {
        RenameConversationDialog(
            initialTitle = row.conversation.title,
            onRename = onRename,
            onDismiss = { renameOpen = false },
        )
    }

    if (deleteOpen) {
        ConfirmDialog(
            title = stringResource(Res.string.confirm_delete_title),
            text = stringResource(Res.string.delete_conversation_q),
            confirmLabel = stringResource(Res.string.delete),
            onConfirm = onDelete,
            onDismiss = { deleteOpen = false },
        )
    }
}

@Composable
private fun ConversationCardMenu(onRename: () -> Unit, onDelete: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menuOpen = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = null)
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.rename)) },
                onClick = {
                    menuOpen = false
                    onRename()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.delete), color = MaterialTheme.colorScheme.error) },
                onClick = {
                    menuOpen = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun RenameConversationDialog(initialTitle: String, onRename: (String) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(initialTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.rename_conversation)) },
        text = {
            OutlinedTextField(value = title, onValueChange = { title = it }, singleLine = true)
        },
        confirmButton = {
            TextButton(onClick = {
                onRename(title)
                onDismiss()
            }) { Text(stringResource(Res.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        },
    )
}
