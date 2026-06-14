package com.phamtunglam.lamity.feature.models.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phamtunglam.lamity.core.presentation.designSystem.components.ConfirmDialog
import com.phamtunglam.lamity.core.presentation.designSystem.formatBytes
import com.phamtunglam.lamity.feature.models.domain.ModelStatus
import com.phamtunglam.lamity.feature.models.domain.ModelWithStatus
import com.phamtunglam.lamity.feature.models.presentation.ModelsViewModel
import com.phamtunglam.lamity.shared.resources.Res
import com.phamtunglam.lamity.shared.resources.chat_action
import com.phamtunglam.lamity.shared.resources.confirm_delete_title
import com.phamtunglam.lamity.shared.resources.configure
import com.phamtunglam.lamity.shared.resources.delete
import com.phamtunglam.lamity.shared.resources.delete_model_file_q
import com.phamtunglam.lamity.shared.resources.download
import com.phamtunglam.lamity.shared.resources.needs_token
import com.phamtunglam.lamity.shared.resources.remove_from_catalog
import com.phamtunglam.lamity.shared.resources.requires_auth_label
import org.jetbrains.compose.resources.stringResource

/** One catalog entry: metadata, transfer state and the actions it allows. */
@Composable
internal fun ModelCard(
    row: ModelWithStatus,
    viewModel: ModelsViewModel,
    onOpenChat: () -> Unit,
    onConfigureModel: (modelId: String) -> Unit,
) {
    val model = row.model
    var confirmDeleteFile by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(model.name, style = MaterialTheme.typography.titleMedium)
                        if (model.requiresAuth) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = stringResource(Res.string.requires_auth_label),
                                modifier = Modifier.padding(top = 2.dp),
                                tint = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                    Text(
                        "${formatBytes(model.sizeBytes)} • ${model.config.backend.name}" +
                            if (model.supportsThinking) " • 💭" else "",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (model.isCustom) {
                    CustomModelMenu(onRemove = { viewModel.removeCustomModel(model) })
                }
            }
            if (model.description.isNotBlank()) {
                Text(
                    model.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            when (row.status) {
                ModelStatus.Downloaded -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = {
                            viewModel.selectForChat(model)
                            onOpenChat()
                        }) { Text(stringResource(Res.string.chat_action)) }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { onConfigureModel(model.id) }) {
                            Text(stringResource(Res.string.configure))
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { confirmDeleteFile = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(Res.string.delete),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
                ModelStatus.NotDownloaded -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { viewModel.download(model) }) { Text(stringResource(Res.string.download)) }
                        if (row.needsToken) {
                            Spacer(Modifier.width(10.dp))
                            Text(
                                stringResource(Res.string.needs_token),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                else -> DownloadPanel(
                    status = row.status,
                    onPause = { viewModel.pauseDownload(model) },
                    onResume = { viewModel.resumeDownload(model) },
                    onCancel = { viewModel.cancelDownload(model) },
                    onRetry = { viewModel.download(model) },
                    onDismissError = { viewModel.dismissError(model) },
                )
            }
        }
    }

    if (confirmDeleteFile) {
        ConfirmDialog(
            title = stringResource(Res.string.confirm_delete_title),
            text = stringResource(Res.string.delete_model_file_q),
            confirmLabel = stringResource(Res.string.delete),
            onConfirm = { viewModel.deleteFile(model) },
            onDismiss = { confirmDeleteFile = false },
        )
    }
}

@Composable
private fun CustomModelMenu(onRemove: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menuOpen = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = null)
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.remove_from_catalog)) },
                onClick = {
                    menuOpen = false
                    onRemove()
                },
            )
        }
    }
}
