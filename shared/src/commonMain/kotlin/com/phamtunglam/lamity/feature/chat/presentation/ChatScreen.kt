package com.phamtunglam.lamity.feature.chat.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.phamtunglam.lamity.core.presentation.designSystem.components.EmptyState
import com.phamtunglam.lamity.feature.chat.domain.EngineState
import com.phamtunglam.lamity.feature.chat.presentation.components.ChatCustomizeSheet
import com.phamtunglam.lamity.feature.chat.presentation.components.ChatErrorBanner
import com.phamtunglam.lamity.feature.chat.presentation.components.ChatInputBar
import com.phamtunglam.lamity.feature.chat.presentation.components.ChatMessageList
import com.phamtunglam.lamity.feature.chat.presentation.components.ChatNoticeBanner
import com.phamtunglam.lamity.feature.chat.presentation.components.EngineLoadingBar
import com.phamtunglam.lamity.shared.resources.Res
import com.phamtunglam.lamity.shared.resources.back
import com.phamtunglam.lamity.shared.resources.chat_empty_body
import com.phamtunglam.lamity.shared.resources.chat_empty_title
import com.phamtunglam.lamity.shared.resources.customize
import com.phamtunglam.lamity.shared.resources.go_to_models
import com.phamtunglam.lamity.shared.resources.new_chat
import com.phamtunglam.lamity.shared.resources.no_model_body
import com.phamtunglam.lamity.shared.resources.no_model_title
import com.phamtunglam.lamity.shared.resources.tab_chat
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String?,
    onBack: () -> Unit,
    onGoToModels: () -> Unit,
    viewModel: ChatViewModel = koinViewModel { parametersOf(conversationId) },
) {
    val ui by viewModel.uiState.collectAsState()
    val state = ui.chat
    var showSheet by remember { mutableStateOf(false) }

    // Warm the engine + native conversation as soon as the chat opens.
    LaunchedEffect(Unit) { viewModel.prepare() }

    val agent = ui.agents.firstOrNull { it.id == state.agentId }
    val title = agent?.name ?: stringResource(Res.string.tab_chat)

    Column(Modifier.fillMaxSize().imePadding()) {
        ChatTopBar(
            title = title,
            onBack = onBack,
            onNewChat = viewModel::newChat,
            onCustomize = { showSheet = true },
        )
        HorizontalDivider()

        Box(Modifier.weight(1f).fillMaxWidth()) {
            val showStreaming =
                state.isGenerating ||
                    state.streamingText.isNotEmpty() || state.streamingThought.isNotEmpty()
            if (state.messages.isEmpty() && !showStreaming) {
                ChatEmptyState(
                    selectedModelReady = ui.selectedModelReady,
                    onGoToModels = onGoToModels,
                )
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
        state.notice?.let { notice ->
            ChatNoticeBanner(notice = notice, onDismiss = viewModel::dismissNotice)
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

    if (showSheet) {
        ChatCustomizeHost(ui = ui, viewModel = viewModel, onDismiss = { showSheet = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    title: String,
    onBack: () -> Unit,
    onNewChat: () -> Unit,
    onCustomize: () -> Unit,
) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
            }
        },
        actions = {
            IconButton(onClick = onNewChat) {
                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.new_chat))
            }
            IconButton(onClick = onCustomize) {
                Icon(Icons.Default.Build, contentDescription = stringResource(Res.string.customize))
            }
        },
    )
}

@Composable
private fun ChatCustomizeHost(ui: ChatUiState, viewModel: ChatViewModel, onDismiss: () -> Unit) {
    val state = ui.chat
    ChatCustomizeSheet(
        agents = ui.agents,
        models = ui.models,
        selectedAgentId = state.agentId,
        selectedModelId = state.modelId,
        customSystemPrompt = state.customSystemPrompt,
        runtimeConfig = state.runtimeConfig,
        onSelectAgent = viewModel::selectAgent,
        onSelectModel = viewModel::selectModel,
        onSetSystemPrompt = viewModel::setCustomSystemPrompt,
        onSetConfig = viewModel::setRuntimeConfig,
        onDismiss = onDismiss,
    )
}

@Composable
private fun ChatEmptyState(selectedModelReady: Boolean, onGoToModels: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!selectedModelReady) {
            EmptyState(stringResource(Res.string.no_model_title), stringResource(Res.string.no_model_body))
            Button(onClick = onGoToModels) {
                Text(stringResource(Res.string.go_to_models))
            }
        } else {
            EmptyState(
                stringResource(Res.string.chat_empty_title),
                stringResource(Res.string.chat_empty_body),
            )
        }
    }
}
