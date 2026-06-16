package com.phamtunglam.lamity.feature.chat.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import com.phamtunglam.lamity.core.presentation.designSystem.components.EmptyState
import com.phamtunglam.lamity.feature.chat.domain.EngineState
import com.phamtunglam.lamity.feature.chat.presentation.components.ChatErrorBanner
import com.phamtunglam.lamity.feature.chat.presentation.components.ChatInputBar
import com.phamtunglam.lamity.feature.chat.presentation.components.ChatMessageList
import com.phamtunglam.lamity.feature.chat.presentation.components.ChatNoticeBanner
import com.phamtunglam.lamity.feature.chat.presentation.components.ChatSettingsSheet
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
import com.phamtunglam.lamity.shared.resources.select_model
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String?,
    onBack: () -> Unit,
    onOpenModels: () -> Unit,
    viewModel: ChatViewModel = koinViewModel { parametersOf(conversationId) },
) {
    val ui by viewModel.uiState.collectAsState()
    val state = ui.chat
    var showSheet by remember { mutableStateOf(false) }

    // Warm the engine + native conversation as soon as the chat opens.
    LaunchedEffect(Unit) { viewModel.prepare() }

    Column(Modifier.fillMaxSize().imePadding()) {
        ChatTopBar(
            modelName = ui.selectedModel?.name,
            onBack = onBack,
            onOpenModels = onOpenModels,
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
                    onGoToModels = onOpenModels,
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
        ChatSettingsHost(ui = ui, viewModel = viewModel, onDismiss = { showSheet = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    modelName: String?,
    onBack: () -> Unit,
    onOpenModels: () -> Unit,
    onNewChat: () -> Unit,
    onCustomize: () -> Unit,
) {
    TopAppBar(
        title = { ModelChip(modelName = modelName, onClick = onOpenModels) },
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

/** App-bar pill showing the active model; tapping it opens the Models screen to pick another. */
@Composable
private fun ModelChip(modelName: String?, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = CircleShape,
    ) {
        Row(
            Modifier
                .clickable(onClick = onClick)
                .padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modelName ?: stringResource(Res.string.select_model),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 220.dp),
            )
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(Res.string.select_model))
        }
    }
}

@Composable
private fun ChatSettingsHost(ui: ChatUiState, viewModel: ChatViewModel, onDismiss: () -> Unit) {
    val state = ui.chat
    ChatSettingsSheet(
        model = ui.selectedModel,
        runtimeConfig = state.runtimeConfig,
        customSystemPrompt = state.customSystemPrompt,
        tools = ui.tools,
        skills = ui.skills,
        enabledToolIds = state.enabledToolIds,
        enabledSkillIds = state.enabledSkillIds,
        onSetConfig = viewModel::setRuntimeConfig,
        onSetSystemPrompt = viewModel::setCustomSystemPrompt,
        onToggleTool = viewModel::toggleTool,
        onToggleSkill = viewModel::toggleSkill,
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
