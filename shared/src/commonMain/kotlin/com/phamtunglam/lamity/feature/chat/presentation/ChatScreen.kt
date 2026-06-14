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
import com.phamtunglam.lamity.feature.chat.presentation.components.ChatErrorBanner
import com.phamtunglam.lamity.feature.chat.presentation.components.ChatHeader
import com.phamtunglam.lamity.feature.chat.presentation.components.ChatInputBar
import com.phamtunglam.lamity.feature.chat.presentation.components.ChatMessageList
import com.phamtunglam.lamity.feature.chat.presentation.components.EngineLoadingBar
import com.phamtunglam.lamity.llm.EngineState
import com.phamtunglam.lamity.shared.resources.Res
import com.phamtunglam.lamity.shared.resources.chat_empty_body
import com.phamtunglam.lamity.shared.resources.chat_empty_title
import com.phamtunglam.lamity.shared.resources.go_to_models
import com.phamtunglam.lamity.shared.resources.no_model_body
import com.phamtunglam.lamity.shared.resources.no_model_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChatScreen(
    onGoToModels: () -> Unit,
    viewModel: ChatViewModel = koinViewModel(),
) {
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
                        EmptyState(stringResource(Res.string.no_model_title), stringResource(Res.string.no_model_body))
                        Button(onClick = onGoToModels) {
                            Text(stringResource(Res.string.go_to_models))
                        }
                    } else {
                        EmptyState(stringResource(Res.string.chat_empty_title), stringResource(Res.string.chat_empty_body))
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
