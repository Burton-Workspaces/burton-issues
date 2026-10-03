package com.burton.issues.ui.issues

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.issues.domain.IssueStateFilter
import com.burton.issues.ui.components.EmptyStatePanel
import com.burton.issues.ui.components.IssueRow
import com.burton.issues.ui.components.RowsSkeleton
import com.burton.issues.ui.theme.BurtonCharcoal
import com.burton.issues.ui.theme.BurtonElevated
import com.burton.issues.ui.theme.BurtonIvory
import com.burton.issues.ui.theme.BurtonMute
import com.burton.issues.ui.theme.BurtonSand

@Composable
fun IssueListScreen(
    onBack: () -> Unit,
    onOpenIssue: (Int) -> Unit,
    onCompose: () -> Unit,
    viewModel: IssueListViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Back", tint = BurtonIvory)
            }
            Text(
                ui.appName,
                style = MaterialTheme.typography.headlineLarge,
                color = BurtonIvory,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onCompose) {
                Icon(Icons.Rounded.Add, contentDescription = "New issue", tint = BurtonIvory)
            }
        }
        Text(ui.repo, style = MaterialTheme.typography.bodyMedium, color = BurtonMute)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip("Open", ui.filter == IssueStateFilter.OPEN) { viewModel.setFilter(IssueStateFilter.OPEN) }
            FilterChip("Closed", ui.filter == IssueStateFilter.CLOSED) { viewModel.setFilter(IssueStateFilter.CLOSED) }
            FilterChip("All", ui.filter == IssueStateFilter.ALL) { viewModel.setFilter(IssueStateFilter.ALL) }
        }
        Spacer(Modifier.height(16.dp))
        when {
            ui.scanning && ui.issues.isEmpty() -> RowsSkeleton()
            ui.issues.isEmpty() && ui.error == null -> {
                val title = when (ui.filter) {
                    IssueStateFilter.OPEN -> "Everything is up to date"
                    IssueStateFilter.CLOSED -> "No closed issues"
                    IssueStateFilter.ALL -> "No issues yet"
                }
                EmptyStatePanel(
                    title = title,
                    action = if (ui.filter == IssueStateFilter.OPEN) "New issue" else null,
                    onAction = if (ui.filter == IssueStateFilter.OPEN) onCompose else null,
                )
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (ui.error != null) {
                        item { Text(ui.error ?: "", color = BurtonIvory) }
                    }
                    items(ui.issues, key = { it.key }) { issue ->
                        IssueRow(issue = issue, onClick = { onOpenIssue(issue.number) }, showRepo = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) BurtonIvory else BurtonMute,
        modifier = Modifier
            .background(if (selected) BurtonElevated else BurtonCharcoal, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}
