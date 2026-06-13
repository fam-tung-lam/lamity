package com.phamtunglam.lamity.feature.chat.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phamtunglam.lamity.feature.chat.domain.ChatMessage
import com.phamtunglam.lamity.feature.chat.domain.MessageRole

/** Conversation transcript pinned to the latest message while streaming. */
@Composable
internal fun ChatMessageList(
    messages: List<ChatMessage>,
    streamingText: String,
    streamingThought: String,
    showStreaming: Boolean,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(messages, key = { it.id }) { message ->
            when (message.role) {
                MessageRole.USER -> UserBubble(message.content)
                MessageRole.ASSISTANT -> AssistantBubble(message)
                MessageRole.TOOL -> ToolCallCard(message)
            }
        }
        if (showStreaming) {
            item(key = "streaming") {
                StreamingBubble(text = streamingText, thought = streamingThought)
            }
        }
    }
    LaunchedEffect(messages.size, streamingText.length, streamingThought.length) {
        val count = listState.layoutInfo.totalItemsCount
        if (count > 0) listState.scrollToItem(count - 1)
    }
}
