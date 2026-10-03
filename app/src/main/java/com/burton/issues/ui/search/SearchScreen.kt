package com.burton.issues.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.issues.ui.components.EmptyStatePanel
import com.burton.issues.ui.components.IssueRow
import com.burton.issues.ui.theme.BurtonCharcoal
import com.burton.issues.ui.theme.BurtonIvory
import com.burton.issues.ui.theme.BurtonMute

@Composable
fun SearchScreen(
    onOpenIssue: (String, Int) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Text("Search", style = MaterialTheme.typography.headlineLarge, color = BurtonIvory)
        Spacer(Modifier.height(12.dp))
        BasicTextField(
            value = ui.query,
            onValueChange = viewModel::onQuery,
            singleLine = true,
            textStyle = MaterialTheme.typography.titleLarge.copy(color = BurtonIvory),
            cursorBrush = SolidColor(BurtonIvory),
            modifier = Modifier
                .fillMaxWidth()
                .background(BurtonCharcoal, RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            decorationBox = { inner ->
                if (ui.query.isBlank()) {
                    Text("Search issues", color = BurtonMute, style = MaterialTheme.typography.titleLarge)
                }
                inner()
            },
        )
        Spacer(Modifier.height(16.dp))
        when {
            ui.query.isBlank() -> EmptyStatePanel(title = "Search subscribed apps")
            ui.hits.isEmpty() && !ui.scanning -> EmptyStatePanel(title = "No matching issues")
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (ui.error != null) {
                        item { Text(ui.error ?: "", color = BurtonIvory) }
                    }
                    items(ui.hits, key = { it.key }) { issue ->
                        IssueRow(issue = issue, onClick = { onOpenIssue(issue.repo, issue.number) })
                    }
                }
            }
        }
    }
}
