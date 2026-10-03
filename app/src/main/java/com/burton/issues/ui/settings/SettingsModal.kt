package com.burton.issues.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.issues.BuildConfig
import com.burton.issues.ui.components.FullScreenModal
import com.burton.issues.ui.theme.BurtonCharcoal
import com.burton.issues.ui.theme.BurtonDanger
import com.burton.issues.ui.theme.BurtonIvory
import com.burton.issues.ui.theme.BurtonMute

@Composable
fun SettingsModal(
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val snapshot by viewModel.state.collectAsStateWithLifecycle()
    val account = snapshot.account
    FullScreenModal(
        onDismiss = onDismiss,
        title = "Settings",
    ) {
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
            subtitle = "GitHub issues. Other trackers can plug in later.",
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
        Spacer(Modifier.height(10.dp))
        SettingsRow(
            title = "Burton Issues",
            subtitle = "About",
            trailing = BuildConfig.VERSION_NAME,
        )
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: String? = null,
    destructive: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BurtonCharcoal, RoundedCornerShape(18.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
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
    }
}
