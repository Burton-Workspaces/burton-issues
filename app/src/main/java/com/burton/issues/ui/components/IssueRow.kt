package com.burton.issues.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.burton.issues.domain.IssueSummary
import com.burton.issues.domain.TimeText
import com.burton.issues.ui.theme.BurtonCharcoal
import com.burton.issues.ui.theme.BurtonIvory
import com.burton.issues.ui.theme.BurtonMute
import com.burton.issues.ui.theme.BurtonSand

@Composable
fun IssueRow(
    issue: IssueSummary,
    onClick: () -> Unit,
    showRepo: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BurtonCharcoal, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = issue.title,
                style = MaterialTheme.typography.titleMedium,
                color = BurtonIvory,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            val meta = buildString {
                if (showRepo) {
                    append(issue.repo.substringAfterLast('/'))
                    append(" · ")
                }
                append("#${issue.number}")
                if (issue.labels.isNotEmpty()) {
                    append(" · ")
                    append(issue.labels.take(3).joinToString(", ") { it.name })
                }
            }
            Text(
                text = meta,
                style = MaterialTheme.typography.bodyMedium,
                color = BurtonMute,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (issue.open) "Open" else "Closed",
                style = MaterialTheme.typography.labelLarge,
                color = BurtonSand,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = TimeText.short(issue.updatedAt.ifBlank { issue.createdAt }),
                style = MaterialTheme.typography.bodyMedium,
                color = BurtonMute,
            )
        }
    }
}
