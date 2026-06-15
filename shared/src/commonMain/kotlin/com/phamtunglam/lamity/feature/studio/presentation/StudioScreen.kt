package com.phamtunglam.lamity.feature.studio.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phamtunglam.lamity.core.presentation.designSystem.components.ConfirmDialog
import com.phamtunglam.lamity.feature.studio.domain.Agent
import com.phamtunglam.lamity.feature.studio.domain.Skill
import com.phamtunglam.lamity.shared.resources.Res
import com.phamtunglam.lamity.shared.resources.agents_caption
import com.phamtunglam.lamity.shared.resources.agents_tab
import com.phamtunglam.lamity.shared.resources.confirm_delete_title
import com.phamtunglam.lamity.shared.resources.delete
import com.phamtunglam.lamity.shared.resources.delete_agent_q
import com.phamtunglam.lamity.shared.resources.delete_skill_q
import com.phamtunglam.lamity.shared.resources.edit
import com.phamtunglam.lamity.shared.resources.new_agent
import com.phamtunglam.lamity.shared.resources.new_skill
import com.phamtunglam.lamity.shared.resources.skills_caption
import com.phamtunglam.lamity.shared.resources.skills_count
import com.phamtunglam.lamity.shared.resources.skills_tab
import com.phamtunglam.lamity.shared.resources.tools_caption
import com.phamtunglam.lamity.shared.resources.tools_count
import com.phamtunglam.lamity.shared.resources.tools_tab
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun StudioScreen(
    onEditAgent: (agentId: String?) -> Unit,
    onEditSkill: (skillId: String?) -> Unit,
    viewModel: StudioViewModel = koinViewModel(),
) {
    val ui by viewModel.uiState.collectAsState()
    var tabIndex by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tabIndex) {
            Tab(
                selected = tabIndex == 0,
                onClick = { tabIndex = 0 },
                text = { Text(stringResource(Res.string.agents_tab)) },
            )
            Tab(
                selected = tabIndex == 1,
                onClick = { tabIndex = 1 },
                text = { Text(stringResource(Res.string.skills_tab)) },
            )
            Tab(
                selected = tabIndex == 2,
                onClick = { tabIndex = 2 },
                text = { Text(stringResource(Res.string.tools_tab)) },
            )
        }
        when (tabIndex) {
            0 -> AgentsTab(ui, viewModel, onEditAgent)
            1 -> SkillsTab(ui, viewModel, onEditSkill)
            else -> ToolsTab(ui, viewModel)
        }
    }
}

@Composable
private fun Caption(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

// ----------------------------------------------------------------- agents

@Composable
private fun AgentsTab(ui: StudioUiState, viewModel: StudioViewModel, onEditAgent: (agentId: String?) -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Caption(stringResource(Res.string.agents_caption))
            LazyColumn(
                contentPadding = PaddingValues(12.dp, 0.dp, 12.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(ui.agents, key = { it.id }) { agent ->
                    AgentCard(
                        agent = agent,
                        onEdit = { onEditAgent(agent.id) },
                        onDelete = { viewModel.deleteAgent(agent.id) },
                    )
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { onEditAgent(null) },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text(stringResource(Res.string.new_agent)) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }
}

@Composable
private fun AgentCard(agent: Agent, onEdit: () -> Unit, onDelete: () -> Unit) {
    var deleteOpen by remember { mutableStateOf(false) }

    Card(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(agent.name, style = MaterialTheme.typography.titleSmall)
                if (agent.description.isNotBlank()) {
                    Text(
                        agent.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "${agent.toolIds.size} ${stringResource(
                        Res.string.tools_count,
                    )} • ${agent.skillIds.size} ${stringResource(Res.string.skills_count)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.edit))
            }
            IconButton(onClick = { deleteOpen = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(Res.string.delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    if (deleteOpen) {
        ConfirmDialog(
            title = stringResource(Res.string.confirm_delete_title),
            text = stringResource(Res.string.delete_agent_q),
            confirmLabel = stringResource(Res.string.delete),
            onConfirm = onDelete,
            onDismiss = { deleteOpen = false },
        )
    }
}

// ----------------------------------------------------------------- skills

@Composable
private fun SkillsTab(ui: StudioUiState, viewModel: StudioViewModel, onEditSkill: (skillId: String?) -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Caption(stringResource(Res.string.skills_caption))
            LazyColumn(
                contentPadding = PaddingValues(12.dp, 0.dp, 12.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(ui.skills, key = { it.id }) { skill ->
                    SkillCard(
                        skill = skill,
                        onEdit = { onEditSkill(skill.id) },
                        onSetEnabled = { viewModel.setSkillEnabled(skill.id, it) },
                        onDelete = { viewModel.deleteSkill(skill.id) },
                    )
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { onEditSkill(null) },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text(stringResource(Res.string.new_skill)) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }
}

@Composable
private fun SkillCard(
    skill: Skill,
    onEdit: () -> Unit,
    onSetEnabled: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    var deleteOpen by remember { mutableStateOf(false) }

    Card(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(skill.name, style = MaterialTheme.typography.titleSmall)
                if (skill.description.isNotBlank()) {
                    Text(
                        skill.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Switch(
                checked = skill.enabled,
                onCheckedChange = onSetEnabled,
            )
            IconButton(onClick = { deleteOpen = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(Res.string.delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    if (deleteOpen) {
        ConfirmDialog(
            title = stringResource(Res.string.confirm_delete_title),
            text = stringResource(Res.string.delete_skill_q),
            confirmLabel = stringResource(Res.string.delete),
            onConfirm = onDelete,
            onDismiss = { deleteOpen = false },
        )
    }
}

// ----------------------------------------------------------------- tools

@Composable
private fun ToolsTab(ui: StudioUiState, viewModel: StudioViewModel) {
    Column(Modifier.fillMaxSize()) {
        Caption(stringResource(Res.string.tools_caption))
        LazyColumn(
            contentPadding = PaddingValues(12.dp, 0.dp, 12.dp, 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(ui.tools, key = { it.id }) { tool ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(tool.displayName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                tool.id,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                tool.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = ui.toolEnabled[tool.id] ?: true,
                            onCheckedChange = { viewModel.setToolEnabled(tool.id, it) },
                        )
                    }
                }
            }
        }
    }
}
