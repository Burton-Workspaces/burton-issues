package com.burton.issues.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.issues.BuildConfig
import com.burton.issues.report.BurtonIssues
import com.burton.issues.domain.TrackedApp
import com.burton.issues.ui.components.EmptyStatePanel
import com.burton.issues.ui.components.FullScreenModal
import com.burton.issues.ui.theme.BurtonCharcoal
import com.burton.issues.ui.theme.BurtonDanger
import com.burton.issues.ui.theme.BurtonGraphite
import com.burton.issues.ui.theme.BurtonIvory
import com.burton.issues.ui.theme.BurtonLine
import com.burton.issues.ui.theme.BurtonMute
import com.burton.issues.ui.theme.BurtonSand
import com.burton.issues.ui.theme.BurtonVoid

enum class SettingsDestination {
    Root,
    Backend,
    TrackedApps,
    About,
}

private data class BackendChoice(val id: String, val name: String)

private val backendChoices = listOf(
    BackendChoice("github", "GitHub"),
)

private fun backendName(id: String): String =
    backendChoices.firstOrNull { it.id == id }?.name ?: "GitHub"

@Composable
fun SettingsModal(
    onDismiss: () -> Unit,
    startAt: SettingsDestination = SettingsDestination.Root,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val snapshot by viewModel.state.collectAsStateWithLifecycle()
    val account = snapshot.account
    val context = LocalContext.current
    var page by remember { mutableStateOf(startAt) }
    LaunchedEffect(startAt) { page = startAt }
    FullScreenModal(
        onDismiss = { if (page == SettingsDestination.Root) onDismiss() else page = SettingsDestination.Root },
        title = when (page) {
            SettingsDestination.Root -> "Settings"
            SettingsDestination.Backend -> "Backend"
            SettingsDestination.TrackedApps -> "Tracked apps"
            SettingsDestination.About -> "About"
        },
        trailing = {
            if (page == SettingsDestination.TrackedApps) {
                IconButton(onClick = viewModel::refresh) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh catalog", tint = BurtonIvory)
                }
            }
        },
    ) {
        when (page) {
            SettingsDestination.Root -> {
                Spacer(Modifier.height(20.dp))
                SettingsRow(
                    title = account?.display ?: "Account",
                    subtitle = when {
                        account == null -> "Not connected"
                        account.email.isNotBlank() -> account.email
                        else -> "@${account.login} · GitHub"
                    },
                )
                Spacer(Modifier.height(10.dp))
                SettingsRow(
                    title = "Backend",
                    subtitle = backendName(snapshot.backendId),
                    onClick = { page = SettingsDestination.Backend },
                )
                Spacer(Modifier.height(10.dp))
                SettingsRow(
                    title = "Tracked apps",
                    subtitle = when {
                        snapshot.catalog.isEmpty() -> "Choose which apps appear in Inbox"
                        snapshot.subscribed.isEmpty() -> "None selected"
                        else -> "${snapshot.subscribed.size} of ${snapshot.catalog.size}"
                    },
                    onClick = { page = SettingsDestination.TrackedApps },
                )
                Spacer(Modifier.height(10.dp))
                SettingsRow(
                    title = "Background refresh",
                    subtitle = "Update Inbox while the app is closed",
                    checked = snapshot.backgroundRefresh,
                    onClick = { viewModel.setBackgroundRefresh(!snapshot.backgroundRefresh) },
                )
                Spacer(Modifier.height(10.dp))
                SettingsRow(
                    title = "About",
                    subtitle = "Burton Issues",
                    trailing = BuildConfig.VERSION_NAME,
                    onClick = { page = SettingsDestination.About },
                    onLongClick = { BurtonIssues.openNewIssue(context) },
                )
                Spacer(Modifier.height(10.dp))
                SettingsRow(
                    title = "Sign out",
                    subtitle = "Remove the token from this phone",
                    destructive = true,
                    onClick = {
                        viewModel.signOut()
                        onDismiss()
                    },
                )
            }
            SettingsDestination.Backend -> {
                Spacer(Modifier.height(12.dp))
                backendChoices.forEach { choice ->
                    BackendChoiceRow(
                        name = choice.name,
                        selected = snapshot.backendId == choice.id,
                        onClick = { viewModel.setBackend(choice.id) },
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
            SettingsDestination.TrackedApps -> {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Inbox only shows issues from apps you track.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BurtonMute,
                )
                Spacer(Modifier.height(16.dp))
                when {
                    snapshot.catalog.isEmpty() && snapshot.scanning -> {
                        Text("Loading catalog…", color = BurtonMute, style = MaterialTheme.typography.bodyMedium)
                    }
                    snapshot.catalog.isEmpty() -> {
                        EmptyStatePanel(
                            title = "No public Android apps found",
                            action = "Refresh catalog",
                            onAction = viewModel::refresh,
                        )
                    }
                    else -> {
                        snapshot.catalog.forEach { app ->
                            TrackedAppRow(
                                app = app,
                                onToggle = { viewModel.setSubscribed(app.repo, !app.subscribed) },
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            }
            SettingsDestination.About -> {
                Spacer(Modifier.height(20.dp))
                SettingsRow(
                    title = "Burton Issues",
                    subtitle = "Version ${BuildConfig.VERSION_NAME}",
                    onLongClick = { BurtonIssues.openNewIssue(context) },
                )
                Spacer(Modifier.height(10.dp))
                SettingsRow(
                    title = "What it does",
                    subtitle = "File, search, and close GitHub issues for public Android apps in Burton-Workspaces. Other Burton apps can open New issue with themselves already selected.",
                )
                Spacer(Modifier.height(10.dp))
                SettingsRow(
                    title = "Backend",
                    subtitle = "GitHub. The token stays on this phone.",
                )
                Spacer(Modifier.height(10.dp))
                SettingsRow(
                    title = "Source",
                    subtitle = "github.com/Burton-Workspaces/burton-issues",
                )
            }
        }
    }
}

@Composable
private fun BackendChoiceRow(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BurtonCharcoal, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = BurtonSand,
                unselectedColor = BurtonMute,
            ),
        )
        Text(
            name,
            style = MaterialTheme.typography.titleMedium,
            color = BurtonIvory,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
        )
    }
}

@Composable
private fun TrackedAppRow(
    app: TrackedApp,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BurtonCharcoal, RoundedCornerShape(18.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = app.subscribed,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = BurtonSand,
                uncheckedColor = BurtonMute,
                checkmarkColor = BurtonVoid,
            ),
        )
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                app.name,
                style = MaterialTheme.typography.titleMedium,
                color = BurtonIvory,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            val bits = buildList {
                add(app.repo)
                if (app.installed) add("Installed")
            }
            Text(
                bits.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = BurtonMute,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    trailing: String? = null,
    checked: Boolean? = null,
    destructive: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BurtonCharcoal, RoundedCornerShape(18.dp))
            .then(
                when {
                    onClick != null && onLongClick != null -> {
                        Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                    }
                    onClick != null -> Modifier.clickable(onClick = onClick)
                    onLongClick != null -> Modifier.combinedClickable(onClick = {}, onLongClick = onLongClick)
                    else -> Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = if (destructive) BurtonDanger else BurtonIvory,
            )
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = BurtonMute)
        }
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.bodyLarge, color = BurtonMute)
        }
        if (checked != null) {
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = BurtonVoid,
                    checkedTrackColor = BurtonSand,
                    uncheckedThumbColor = BurtonIvory,
                    uncheckedTrackColor = BurtonGraphite,
                    uncheckedBorderColor = BurtonLine,
                ),
            )
        } else if (onClick != null && !destructive) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = BurtonMute,
            )
        }
    }
}
