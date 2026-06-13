package com.phamtunglam.lamity.feature.models.presentation.components

import androidx.compose.foundation.layout.Column
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
import com.phamtunglam.lamity.core.designsystem.formatEta
import com.phamtunglam.lamity.core.designsystem.formatPercent
import com.phamtunglam.lamity.core.designsystem.formatSpeed
import com.phamtunglam.lamity.core.i18n.LocalStrings
import com.phamtunglam.lamity.feature.models.domain.ModelStatus

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
    val str = LocalStrings.current
    when (status) {
        is ModelStatus.Queued -> {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            StatusRow(label = str.downloadQueued) {
                TextButton(onClick = onCancel) { Text(str.cancel) }
            }
        }
        is ModelStatus.Downloading -> {
            ProgressBar(status.downloadedBytes, status.totalBytes)
            val rate = listOfNotNull(
                formatSpeed(status.bytesPerSecond),
                formatEta(status.etaMillis),
            ).joinToString(" • ")
            StatusRow(
                label = formatPercent(status.downloadedBytes, status.totalBytes) +
                    if (rate.isNotEmpty()) "\n$rate" else "",
            ) {
                TextButton(onClick = onPause) { Text(str.pause) }
                TextButton(onClick = onCancel) { Text(str.cancel) }
            }
        }
        is ModelStatus.Paused -> {
            ProgressBar(status.downloadedBytes, status.totalBytes)
            StatusRow(label = "${str.paused} • ${formatPercent(status.downloadedBytes, status.totalBytes)}") {
                TextButton(onClick = onResume) { Text(str.resume) }
                TextButton(onClick = onCancel) { Text(str.cancel) }
            }
        }
        is ModelStatus.Verifying -> {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            StatusRow(label = str.verifying) {}
        }
        is ModelStatus.Failed -> {
            Text(
                status.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            Row {
                TextButton(onClick = onRetry) { Text(str.retry) }
                TextButton(onClick = onDismissError) { Text(str.dismiss) }
            }
        }
        ModelStatus.NotDownloaded, ModelStatus.Downloaded -> Unit // handled by the card itself
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
