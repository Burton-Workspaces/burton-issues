package com.burton.issues.ui.composeissue

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.issues.ui.theme.BurtonBlack
import com.burton.issues.ui.theme.BurtonCharcoal
import com.burton.issues.ui.theme.BurtonIvory
import com.burton.issues.ui.theme.BurtonMute
import com.burton.issues.ui.theme.BurtonSand
import com.burton.issues.ui.theme.BurtonVoid

@Composable
fun ComposeIssueScreen(
    onBack: () -> Unit,
    onCreated: (String, Int) -> Unit,
    viewModel: ComposeIssueViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    LaunchedEffect(ui.createdNumber) {
        val number = ui.createdNumber ?: return@LaunchedEffect
        onCreated(ui.repo, number)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BurtonBlack)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Back", tint = BurtonIvory)
            }
            Text("New issue", style = MaterialTheme.typography.headlineLarge, color = BurtonIvory)
        }
        Spacer(Modifier.height(16.dp))
        Text("APP", style = MaterialTheme.typography.labelSmall, color = BurtonMute)
        Spacer(Modifier.height(8.dp))
        ui.apps.ifEmpty { emptyList() }.forEach { app ->
            val selected = app.repo == ui.repo
            Text(
                app.name,
                color = if (selected) BurtonSand else BurtonIvory,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BurtonCharcoal, RoundedCornerShape(14.dp))
                    .clickable { viewModel.setRepo(app.repo) }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .padding(bottom = 8.dp),
            )
            Spacer(Modifier.height(8.dp))
        }
        Field("Title", ui.title, viewModel::onTitle)
        Spacer(Modifier.height(12.dp))
        Field("Body", ui.body, viewModel::onBody, singleLine = false)
        if (ui.error != null) {
            Spacer(Modifier.height(12.dp))
            Text(ui.error ?: "", color = BurtonIvory)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = viewModel::submit,
            enabled = !ui.sending,
            colors = ButtonDefaults.buttonColors(containerColor = BurtonIvory, contentColor = BurtonVoid),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (ui.sending) "Creating…" else "Create issue")
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit, singleLine: Boolean = true) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BurtonCharcoal, RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = BurtonMute)
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = singleLine,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = BurtonIvory),
            cursorBrush = SolidColor(BurtonIvory),
            modifier = Modifier.fillMaxWidth().then(if (singleLine) Modifier else Modifier.height(160.dp)),
            decorationBox = { inner ->
                if (value.isBlank()) Text(label, color = BurtonMute, style = MaterialTheme.typography.bodyLarge)
                inner()
            },
        )
    }
}
