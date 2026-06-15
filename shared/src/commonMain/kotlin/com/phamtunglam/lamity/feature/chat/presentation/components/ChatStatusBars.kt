package com.phamtunglam.lamity.feature.chat.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.phamtunglam.lamity.feature.chat.domain.ChatError
import com.phamtunglam.lamity.feature.chat.domain.ChatErrorKind
import com.phamtunglam.lamity.feature.chat.domain.ChatNotice
import com.phamtunglam.lamity.shared.resources.Res
import com.phamtunglam.lamity.shared.resources.dismiss
import com.phamtunglam.lamity.shared.resources.error_gpu_load_failed
import com.phamtunglam.lamity.shared.resources.error_model_unsupported_on_device
import com.phamtunglam.lamity.shared.resources.loading_model
import com.phamtunglam.lamity.shared.resources.notice_switched_to_cpu
import org.jetbrains.compose.resources.stringResource

/** Indeterminate banner shown while the native engine loads a model. */
@Composable
internal fun EngineLoadingBar() {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(
            stringResource(Res.string.loading_model),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
internal fun ChatErrorBanner(error: ChatError, onDismiss: () -> Unit) {
    StatusBanner(
        text = chatErrorText(error),
        container = MaterialTheme.colorScheme.errorContainer,
        content = MaterialTheme.colorScheme.onErrorContainer,
        onDismiss = onDismiss,
    )
}

/** Informational banner (e.g. a model was switched to the CPU backend), distinct from an error. */
@Composable
internal fun ChatNoticeBanner(notice: ChatNotice, onDismiss: () -> Unit) {
    StatusBanner(
        text = chatNoticeText(notice),
        container = MaterialTheme.colorScheme.secondaryContainer,
        content = MaterialTheme.colorScheme.onSecondaryContainer,
        onDismiss = onDismiss,
    )
}

@Composable
private fun StatusBanner(
    text: String,
    container: Color,
    content: Color,
    onDismiss: () -> Unit,
) {
    Surface(
        color = container,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Row(
            Modifier.padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = content,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.dismiss)) }
        }
    }
}

@Composable
private fun chatErrorText(error: ChatError): String =
    when (error) {
        is ChatError.Raw -> {
            error.message
        }

        is ChatError.Known -> {
            when (error.kind) {
                ChatErrorKind.MODEL_UNSUPPORTED_ON_DEVICE -> {
                    stringResource(Res.string.error_model_unsupported_on_device)
                }

                ChatErrorKind.GPU_LOAD_FAILED -> {
                    stringResource(Res.string.error_gpu_load_failed)
                }
            }
        }
    }

@Composable
private fun chatNoticeText(notice: ChatNotice): String =
    when (notice) {
        ChatNotice.SWITCHED_TO_CPU -> stringResource(Res.string.notice_switched_to_cpu)
    }
