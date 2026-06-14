package com.phamtunglam.lamity.feature.models.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phamtunglam.lamity.shared.resources.Res
import com.phamtunglam.lamity.shared.resources.add
import com.phamtunglam.lamity.shared.resources.add_custom_model
import com.phamtunglam.lamity.shared.resources.cancel
import com.phamtunglam.lamity.shared.resources.custom_model_name
import com.phamtunglam.lamity.shared.resources.custom_model_url
import com.phamtunglam.lamity.shared.resources.requires_auth_label
import org.jetbrains.compose.resources.stringResource

/** Adds a model by direct .litertlm URL to the catalog. */
@Composable
internal fun AddCustomModelDialog(
    onAdd: (name: String, url: String, requiresAuth: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var requiresAuth by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.add_custom_model)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(Res.string.custom_model_name)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(Res.string.custom_model_url)) },
                    singleLine = true,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(Res.string.requires_auth_label), Modifier.weight(1f))
                    Switch(checked = requiresAuth, onCheckedChange = { requiresAuth = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = url.contains("://"),
                onClick = {
                    onAdd(name, url, requiresAuth)
                    onDismiss()
                },
            ) { Text(stringResource(Res.string.add)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}
