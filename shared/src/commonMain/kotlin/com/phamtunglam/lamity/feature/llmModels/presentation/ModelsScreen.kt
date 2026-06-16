package com.phamtunglam.lamity.feature.llmModels.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phamtunglam.lamity.core.presentation.designSystem.components.SubScreenScaffold
import com.phamtunglam.lamity.feature.llmModels.presentation.components.ModelCard
import com.phamtunglam.lamity.shared.resources.Res
import com.phamtunglam.lamity.shared.resources.models_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ModelsScreen(onModelSelected: () -> Unit, onBack: () -> Unit, viewModel: ModelsViewModel = koinViewModel()) {
    val ui by viewModel.uiState.collectAsState()

    SubScreenScaffold(title = stringResource(Res.string.models_title), onBack = onBack) {
        LazyColumn(
            // Bottom padding clears the navigation bar so the last card is reachable.
            contentPadding =
                PaddingValues(
                    start = 12.dp,
                    top = 12.dp,
                    end = 12.dp,
                    bottom = 12.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(ui.rows, key = { it.model.id }) { row ->
                ModelCard(
                    row = row,
                    isSelected = row.model.id == ui.selectedModelId,
                    viewModel = viewModel,
                    onModelSelected = onModelSelected,
                )
            }
        }
    }
}
