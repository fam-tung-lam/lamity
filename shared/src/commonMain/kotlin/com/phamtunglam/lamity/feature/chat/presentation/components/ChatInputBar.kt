package com.phamtunglam.lamity.feature.chat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phamtunglam.lamity.shared.resources.Res
import com.phamtunglam.lamity.shared.resources.chat_placeholder
import com.phamtunglam.lamity.shared.resources.send
import com.phamtunglam.lamity.shared.resources.stop
import org.jetbrains.compose.resources.stringResource

/** Message field plus a send button that morphs into stop while generating. */
@Composable
internal fun ChatInputBar(
    enabled: Boolean,
    isGenerating: Boolean,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            placeholder = { Text(stringResource(Res.string.chat_placeholder)) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            maxLines = 5,
        )
        Spacer(Modifier.width(8.dp))
        if (isGenerating) {
            FilledIconButton(onClick = onStop, modifier = Modifier.size(52.dp)) {
                Box(
                    Modifier.size(14.dp).background(
                        MaterialTheme.colorScheme.onPrimary,
                        RoundedCornerShape(2.dp),
                    ),
                ) { }
            }
        } else {
            FilledIconButton(
                onClick = {
                    val text = input
                    input = ""
                    onSend(text)
                },
                enabled = enabled && input.isNotBlank(),
                modifier = Modifier.size(52.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = if (isGenerating) stringResource(Res.string.stop) else stringResource(Res.string.send),
                )
            }
        }
    }
}
