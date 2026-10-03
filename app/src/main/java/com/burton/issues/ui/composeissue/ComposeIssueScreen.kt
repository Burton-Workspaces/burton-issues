package com.burton.issues.ui.composeissue

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.burton.issues.domain.TrackedApp
import com.burton.issues.ui.theme.BurtonBlack
import com.burton.issues.ui.theme.BurtonCharcoal
import com.burton.issues.ui.theme.BurtonIvory
import com.burton.issues.ui.theme.BurtonMute
import com.burton.issues.ui.theme.BurtonSand
import com.burton.issues.ui.theme.BurtonVoid
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
            .imePadding()
            .padding(horizontal = 20.dp),
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
        AppDropdown(
            apps = ui.apps,
            selectedRepo = ui.repo,
            onSelect = viewModel::setRepo,
        )
        Spacer(Modifier.height(12.dp))
        Field("Title", ui.title, viewModel::onTitle)
        Spacer(Modifier.height(12.dp))
        Field(
            label = "Detail",
            value = ui.body,
            onChange = viewModel::onBody,
            singleLine = false,
            modifier = Modifier.weight(1f),
        )
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
private fun AppDropdown(
    apps: List<TrackedApp>,
    selectedRepo: String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = apps.firstOrNull { it.repo == selectedRepo }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BurtonCharcoal, RoundedCornerShape(14.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selected?.name ?: if (apps.isEmpty()) "Loading apps…" else "Choose an app",
                color = if (selected != null) BurtonIvory else BurtonMute,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "Hide apps" else "Show apps",
                tint = BurtonMute,
            )
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                apps.forEach { app ->
                    val selectedItem = app.repo == selectedRepo
                    Text(
                        text = app.name,
                        color = if (selectedItem) BurtonSand else BurtonIvory,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .background(BurtonCharcoal, RoundedCornerShape(14.dp))
                            .clickable {
                                onSelect(app.repo)
                                expanded = false
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
) {
    val bringIntoView = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    fun reveal() {
        scope.launch {
            delay(280)
            bringIntoView.bringIntoView()
        }
    }
    Column(
        modifier = modifier
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
            onValueChange = onChange,
            singleLine = singleLine,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = BurtonIvory),
            cursorBrush = SolidColor(BurtonIvory),
            modifier = Modifier
                .fillMaxWidth()
                .then(if (singleLine) Modifier else Modifier.weight(1f)),
        )
    }
}
