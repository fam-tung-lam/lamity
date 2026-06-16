package com.phamtunglam.lamity.feature.llmModels.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phamtunglam.lamity.core.presentation.designSystem.formatEta
import com.phamtunglam.lamity.core.presentation.designSystem.formatPercent
import com.phamtunglam.lamity.core.presentation.designSystem.formatSpeed
import com.phamtunglam.lamity.feature.llmModels.domain.ModelStatus
import com.phamtunglam.lamity.shared.resources.Res
import com.phamtunglam.lamity.shared.resources.cancel
import com.phamtunglam.lamity.shared.resources.dismiss
import com.phamtunglam.lamity.shared.resources.download_queued
import com.phamtunglam.lamity.shared.resources.pause
import com.phamtunglam.lamity.shared.resources.paused
import com.phamtunglam.lamity.shared.resources.resume
import com.phamtunglam.lamity.shared.resources.retry
import com.phamtunglam.lamity.shared.resources.verifying
import org.jetbrains.compose.resources.stringResource

/** Transfer status block of a model card: progress, speed/ETA and controls. */
@Composable
internal fun DownloadPanel(
    status: ModelStatus,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDismissError: () -> Unit,
) {
    when (status) {
        is ModelStatus.Queued -> {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            StatusRow(label = stringResource(Res.string.download_queued)) {
                TextButton(onClick = onCancel) { Text(stringResource(Res.string.cancel)) }
            }
        }

        is ModelStatus.Downloading -> {
            ProgressBar(status.downloadedBytes, status.totalBytes)
            val rate =
                listOfNotNull(
                    formatSpeed(status.bytesPerSecond),
                    formatEta(status.etaMillis),
                ).joinToString(" • ")
            StatusRow(
                label =
                    formatPercent(status.downloadedBytes, status.totalBytes) +
                        if (rate.isNotEmpty()) "\n$rate" else "",
            ) {
                TextButton(onClick = onPause) { Text(stringResource(Res.string.pause)) }
                TextButton(onClick = onCancel) { Text(stringResource(Res.string.cancel)) }
            }
        }

        is ModelStatus.Paused -> {
            ProgressBar(status.downloadedBytes, status.totalBytes)
            StatusRow(
                label = "${stringResource(
                    Res.string.paused,
                )} • ${formatPercent(status.downloadedBytes, status.totalBytes)}",
            ) {
                TextButton(onClick = onResume) { Text(stringResource(Res.string.resume)) }
                TextButton(onClick = onCancel) { Text(stringResource(Res.string.cancel)) }
            }
        }

        is ModelStatus.Verifying -> {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            StatusRow(label = stringResource(Res.string.verifying)) {}
        }

        is ModelStatus.Failed -> {
            Text(
                status.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            Row {
                TextButton(onClick = onRetry) { Text(stringResource(Res.string.retry)) }
                TextButton(onClick = onDismissError) { Text(stringResource(Res.string.dismiss)) }
            }
        }

        ModelStatus.NotDownloaded, ModelStatus.Downloaded -> {
            Unit
        } // handled by the card itself
    }
}

@Composable
private fun ProgressBar(downloadedBytes: Long, totalBytes: Long) {
    if (totalBytes > 0) {
        LinearProgressIndicator(
            progress = { (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun StatusRow(label: String, actions: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        actions()
    }
}
