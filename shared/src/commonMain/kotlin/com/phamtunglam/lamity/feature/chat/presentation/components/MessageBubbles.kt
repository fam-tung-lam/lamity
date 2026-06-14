package com.phamtunglam.lamity.feature.chat.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.phamtunglam.lamity.core.presentation.designSystem.theme.LamityTheme
import com.phamtunglam.lamity.core.presentation.designSystem.theme.bubbleShape
import com.phamtunglam.lamity.core.presentation.i18n.LocalStrings
import com.phamtunglam.lamity.feature.chat.domain.ChatMessage
import kotlin.math.roundToInt

@Composable
internal fun UserBubble(text: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(
            color = LamityTheme.colors.userBubble,
            shape = bubbleShape(fromUser = true),
            modifier = Modifier.widthIn(max = 360.dp),
        ) {
            Text(
                text,
                Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = LamityTheme.colors.onUserBubble,
            )
        }
    }
}

@Composable
internal fun AssistantBubble(message: ChatMessage) {
    val str = LocalStrings.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (message.thought.isNotBlank()) {
            ThoughtBlock(message.thought, str.thinkingLabel, initiallyExpanded = false)
        }
        if (message.content.isNotBlank()) {
            AssistantText(message.content)
        }
        if (message.tokensPerSec > 0) {
            Text(
                "≈ ${fmt1(message.tokensPerSec)} tok/s • ${fmt1(message.genMillis / 1000.0)}s",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
internal fun StreamingBubble(text: String, thought: String) {
    val str = LocalStrings.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (thought.isNotBlank()) {
            ThoughtBlock(thought, str.thinkingLabel, initiallyExpanded = true)
        }
        if (text.isBlank()) {
            Surface(
                color = LamityTheme.colors.assistantBubble,
                shape = bubbleShape(fromUser = false),
                modifier = Modifier.widthIn(max = 360.dp),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("…", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            AssistantText(text)
        }
    }
}

@Composable
private fun AssistantText(text: String) {
    Surface(
        color = LamityTheme.colors.assistantBubble,
        shape = bubbleShape(fromUser = false),
        modifier = Modifier.widthIn(max = 360.dp),
    ) {
        Text(
            text,
            Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = LamityTheme.colors.onAssistantBubble,
        )
    }
}

@Composable
private fun ThoughtBlock(thought: String, label: String, initiallyExpanded: Boolean) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    Surface(
        color = LamityTheme.colors.thoughtBubble,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.widthIn(max = 360.dp).clickable { expanded = !expanded },
    ) {
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "💭 $label",
                    style = MaterialTheme.typography.labelMedium,
                    color = LamityTheme.colors.onThoughtBubble,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = LamityTheme.colors.onThoughtBubble,
                )
            }
            if (expanded) {
                Text(
                    thought,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = LamityTheme.colors.onThoughtBubble,
                )
            }
        }
    }
}

private fun fmt1(value: Double): String = ((value * 10).roundToInt() / 10.0).toString()
