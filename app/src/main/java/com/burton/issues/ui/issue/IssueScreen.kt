package com.burton.issues.ui.issue

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.issues.domain.IssueComment
import com.burton.issues.domain.TimeText
import com.burton.issues.ui.components.BurtonModalSheet
import com.burton.issues.ui.components.RowsSkeleton
import com.burton.issues.ui.components.UserAvatar
import com.burton.issues.ui.theme.BurtonBlack
import com.burton.issues.ui.theme.BurtonCharcoal
import com.burton.issues.ui.theme.BurtonIvory
import com.burton.issues.ui.theme.BurtonMute
import com.burton.issues.ui.theme.BurtonSand
import com.burton.issues.ui.theme.BurtonVoid
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun IssueScreen(
    onBack: () -> Unit,
    viewModel: IssueViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var picker by remember { mutableStateOf<String?>(null) }
    val issue = ui.issue
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BurtonBlack)
            .imePadding()
            .padding(horizontal = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Back", tint = BurtonIvory)
            }
            Text(
                if (issue == null) "Issue" else "${ui.repo.substringAfterLast('/')} #${ui.number}",
                style = MaterialTheme.typography.headlineLarge,
                color = BurtonIvory,
                modifier = Modifier.weight(1f),
            )
        }
        when {
            ui.scanning && issue == null -> RowsSkeleton(4)
            issue == null -> Text(ui.error ?: "Could not load this issue.", color = BurtonIvory)
            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        if (ui.editing) {
                            Field("Title", ui.editTitle, viewModel::onEditTitle)
                            Spacer(Modifier.height(8.dp))
                            Field("Body", ui.editBody, viewModel::onEditBody, singleLine = false)
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = viewModel::saveEdit,
                                colors = ButtonDefaults.buttonColors(containerColor = BurtonIvory, contentColor = BurtonVoid),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Save") }
                        } else {
                            Text(issue.title, style = MaterialTheme.typography.titleLarge, color = BurtonIvory)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                buildString {
                                    append(if (issue.open) "Open" else "Closed")
                                    append(" · ${issue.user.login}")
                                    append(" · ${TimeText.short(issue.createdAt)}")
                                    if (issue.labels.isNotEmpty()) {
                                        append(" · ")
                                        append(issue.labels.joinToString(", ") { it.name })
                                    }
                                    issue.milestone?.let { append(" · ${it.title}") }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = BurtonMute,
                            )
                            if (issue.body.isNotBlank()) {
                                Spacer(Modifier.height(12.dp))
                                Text(issue.body, style = MaterialTheme.typography.bodyLarge, color = BurtonIvory)
                            }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { viewModel.setEditing(!ui.editing) }) {
                                Text(if (ui.editing) "Cancel" else "Edit", color = BurtonSand)
                            }
                            TextButton(onClick = viewModel::toggleState) {
                                Text(if (issue.open) "Close" else "Reopen", color = BurtonSand)
                            }
                            TextButton(onClick = { picker = "labels" }) { Text("Labels", color = BurtonSand) }
                            TextButton(onClick = { picker = "assignees" }) { Text("Assignees", color = BurtonSand) }
                        }
                    }
                    items(ui.comments, key = { it.id }) { comment ->
                        CommentCard(
                            comment = comment,
                            canDelete = true,
                            onDelete = { viewModel.deleteComment(comment.id) },
                        )
                    }
                    item {
                        Field("Comment", ui.draft, viewModel::onDraft, singleLine = false)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = viewModel::sendComment,
                            enabled = ui.draft.isNotBlank() && !ui.sending,
                            colors = ButtonDefaults.buttonColors(containerColor = BurtonIvory, contentColor = BurtonVoid),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Comment") }
                        Spacer(Modifier.height(24.dp))
                    }
                    if (ui.error != null) {
                        item { Text(ui.error ?: "", color = BurtonIvory) }
                    }
                }
            }
        }
    }
    when (picker) {
        "labels" -> {
            val selected = issue?.labels?.map { it.name }.orEmpty().toSet()
            PickerSheet(
                title = "Labels",
                options = ui.labels.map { it.name to it.description },
                selected = selected,
                onDismiss = { picker = null },
                onSave = { viewModel.setLabels(it); picker = null },
            )
        }
        "assignees" -> {
            val selected = issue?.assignees?.map { it.login }.orEmpty().toSet()
            PickerSheet(
                title = "Assignees",
                options = ui.assignees.map { it.login to "" },
                selected = selected,
                onDismiss = { picker = null },
                onSave = { viewModel.setAssignees(it); picker = null },
            )
        }
    }
}

@Composable
private fun CommentCard(
    comment: IssueComment,
    canDelete: Boolean,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BurtonCharcoal, RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(url = comment.user.avatarUrl, name = comment.user.login, size = 28.dp)
            Spacer(Modifier.padding(6.dp))
            Text(comment.user.login, color = BurtonIvory, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(TimeText.short(comment.createdAt), color = BurtonMute, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(8.dp))
        Text(comment.body, color = BurtonIvory, style = MaterialTheme.typography.bodyLarge)
        if (canDelete) {
            TextButton(onClick = onDelete) { Text("Delete", color = BurtonSand) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit, singleLine: Boolean = true) {
    val bringIntoView = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    fun reveal() {
        scope.launch {
            delay(280)
            bringIntoView.bringIntoView()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoView)
            .onFocusEvent { if (it.isFocused) reveal() }
            .background(BurtonCharcoal, RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = BurtonMute)
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = value,
            onValueChange = {
                onChange(it)
                reveal()
            },
            singleLine = singleLine,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = BurtonIvory),
            cursorBrush = SolidColor(BurtonIvory),
            modifier = Modifier.fillMaxWidth().then(if (singleLine) Modifier else Modifier.height(120.dp)),
        )
    }
}

@Composable
private fun PickerSheet(
    title: String,
    options: List<Pair<String, String>>,
    selected: Set<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit,
) {
    var current by remember { mutableStateOf(selected) }
    BurtonModalSheet(onDismiss = onDismiss) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = BurtonIvory)
        Spacer(Modifier.height(12.dp))
        options.forEach { (name, hint) ->
            val on = name in current
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        current = if (on) current - name else current + name
                    }
                    .padding(vertical = 10.dp),
            ) {
                Text(name, color = if (on) BurtonSand else BurtonIvory, style = MaterialTheme.typography.titleMedium)
                if (hint.isNotBlank()) Text(hint, color = BurtonMute, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { onSave(current.toList()) },
            colors = ButtonDefaults.buttonColors(containerColor = BurtonIvory, contentColor = BurtonVoid),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save") }
    }
}
