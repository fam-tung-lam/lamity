package com.phamtunglam.lamity.feature.home.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.phamtunglam.lamity.shared.resources.Res
import com.phamtunglam.lamity.shared.resources.agents_count
import com.phamtunglam.lamity.shared.resources.card_agents
import com.phamtunglam.lamity.shared.resources.card_chats
import com.phamtunglam.lamity.shared.resources.card_models
import com.phamtunglam.lamity.shared.resources.card_skills
import com.phamtunglam.lamity.shared.resources.card_tools
import com.phamtunglam.lamity.shared.resources.chats_count
import com.phamtunglam.lamity.shared.resources.home_title
import com.phamtunglam.lamity.shared.resources.models_count
import com.phamtunglam.lamity.shared.resources.open_settings
import com.phamtunglam.lamity.shared.resources.skills_count
import com.phamtunglam.lamity.shared.resources.tools_count
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private data class HomeCardModel(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenAgents: () -> Unit,
    onOpenSkills: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenChats: () -> Unit,
    onOpenModels: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val ui by viewModel.uiState.collectAsState()

    val cards =
        listOf(
            HomeCardModel(
                stringResource(Res.string.card_agents),
                "${ui.agentsCount} ${stringResource(Res.string.agents_count)}",
                Icons.Default.Person,
                onOpenAgents,
            ),
            HomeCardModel(
                stringResource(Res.string.card_skills),
                "${ui.skillsCount} ${stringResource(Res.string.skills_count)}",
                Icons.Default.Star,
                onOpenSkills,
            ),
            HomeCardModel(
                stringResource(Res.string.card_tools),
                "${ui.toolsCount} ${stringResource(Res.string.tools_count)}",
                Icons.Default.Build,
                onOpenTools,
            ),
            HomeCardModel(
                stringResource(Res.string.card_chats),
                "${ui.chatsCount} ${stringResource(Res.string.chats_count)}",
                Icons.Default.DateRange,
                onOpenChats,
            ),
            HomeCardModel(
                stringResource(Res.string.card_models),
                "${ui.modelsCount} ${stringResource(Res.string.models_count)}",
                Icons.Default.Home,
                onOpenModels,
            ),
        )

    Column(Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text(stringResource(Res.string.home_title)) },
            actions = {
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = stringResource(Res.string.open_settings))
                }
            },
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(cards) { card -> HomeCard(card) }
        }
    }
}

@Composable
private fun HomeCard(card: HomeCardModel) {
    Card(
        onClick = card.onClick,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().height(140.dp),
    ) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                card.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(card.title, style = MaterialTheme.typography.titleMedium)
            Text(
                card.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
