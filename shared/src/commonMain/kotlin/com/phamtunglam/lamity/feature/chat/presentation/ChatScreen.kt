package com.phamtunglam.lamity.feature.chat.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.phamtunglam.lamity.core.presentation.designSystem.components.EmptyState
import com.phamtunglam.lamity.core.presentation.i18n.LocalStrings
import com.phamtunglam.lamity.feature.chat.presentation.components.ChatErrorBanner
import com.phamtunglam.lamity.feature.chat.presentation.components.ChatHeader
import com.phamtunglam.lamity.feature.chat.presentation.components.ChatInputBar
import com.phamtunglam.lamity.feature.chat.presentation.components.ChatMessageList
import com.phamtunglam.lamity.feature.chat.presentation.components.EngineLoadingBar
import com.phamtunglam.lamity.llm.EngineState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChatScreen(
    onGoToModels: () -> Unit,
    viewModel: ChatViewModel = koinViewModel(),
) {
    val str = LocalStrings.current
    val ui by viewModel.uiState.collectAsState()
    val state = ui.chat

    Column(Modifier.fillMaxSize().imePadding()) {
        ChatHeader(
            agents = ui.agents,
            models = ui.models,
            selectedAgentId = state.agentId,
            selectedModelId = state.modelId,
            onSelectAgent = viewModel::selectAgent,
            onSelectModel = viewModel::selectModel,
            onNewChat = viewModel::newChat,
        )
        HorizontalDivider()

        Box(Modifier.weight(1f).fillMaxWidth()) {
            val showStreaming = state.isGenerating ||
                state.streamingText.isNotEmpty() || state.streamingThought.isNotEmpty()
            if (state.messages.isEmpty() && !showStreaming) {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (!ui.selectedModelReady) {
                        EmptyState(str.noModelTitle, str.noModelBody)
                        Button(onClick = onGoToModels) {
                            Text(str.goToModels)
                        }
                    } else {
                        EmptyState(str.chatEmptyTitle, str.chatEmptyBody)
                    }
                }
            } else {
                ChatMessageList(
                    messages = state.messages,
                    streamingText = state.streamingText,
                    streamingThought = state.streamingThought,
                    showStreaming = showStreaming,
                )
            }
        }

        if (state.engine is EngineState.Loading) {
            EngineLoadingBar()
        }
        state.error?.let { error ->
            ChatErrorBanner(error = error, onDismiss = viewModel::dismissError)
        }

        ChatInputBar(
            enabled = ui.selectedModelReady,
            isGenerating = state.isGenerating,
            onSend = viewModel::send,
            onStop = viewModel::stopGeneration,
        )
    }
}
