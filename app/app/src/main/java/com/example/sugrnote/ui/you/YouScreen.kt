package com.example.sugrnote.ui.you

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sugrnote.data.settings.DarkThemeStyle
import com.example.sugrnote.data.settings.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouScreen(
    viewModel: YouViewModel,
    onNavigateToSettings: () -> Unit
) {
    val prefs by viewModel.userPreferences.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentMode = prefs.themeMode,
            currentStyle = prefs.darkThemeStyle,
            onModeSelected = { viewModel.setThemeMode(it) },
            onStyleSelected = { viewModel.setDarkThemeStyle(it) },
            onDismiss = { showThemeDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("You") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                "Preferences",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
            )
            ListItem(
                headlineContent = { Text("Manage Glucose Unit and Threshold") },
                leadingContent = {
                    Icon(Icons.Default.Settings, contentDescription = null)
                },
                trailingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToSettings)
            )

            Text(
                "Theme",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
            )
            ListItem(
                headlineContent = { Text("Theme Options") },
                supportingContent = {
                    val themeDesc = when (prefs.themeMode) {
                        ThemeMode.SYSTEM -> "System default"
                        ThemeMode.LIGHT -> "Light"
                        ThemeMode.DARK -> "Dark"
                    }
                    val styleDesc = if (prefs.themeMode != ThemeMode.LIGHT && prefs.darkThemeStyle == DarkThemeStyle.OLED) " (OLED)" else ""
                    Text("$themeDesc$styleDesc")
                },
                leadingContent = { Icon(Icons.Default.Palette, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().clickable { showThemeDialog = true }
            )
        }
    }
}

@Composable
fun ThemeSelectionDialog(
    currentMode: ThemeMode,
    currentStyle: DarkThemeStyle,
    onModeSelected: (ThemeMode) -> Unit,
    onStyleSelected: (DarkThemeStyle) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Theme") },
        text = {
            Column {
                Text("Mode", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = currentMode == ThemeMode.SYSTEM,
                        onClick = { onModeSelected(ThemeMode.SYSTEM) },
                        label = { Text("System") }
                    )
                    FilterChip(
                        selected = currentMode == ThemeMode.LIGHT,
                        onClick = { onModeSelected(ThemeMode.LIGHT) },
                        label = { Text("Light") }
                    )
                    FilterChip(
                        selected = currentMode == ThemeMode.DARK,
                        onClick = { onModeSelected(ThemeMode.DARK) },
                        label = { Text("Dark") }
                    )
                }

                if (currentMode != ThemeMode.LIGHT) {
                    Spacer(Modifier.height(24.dp))
                    Text("Dark Theme Style", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = currentStyle == DarkThemeStyle.STANDARD,
                            onClick = { onStyleSelected(DarkThemeStyle.STANDARD) },
                            label = { Text("Standard") }
                        )
                        FilterChip(
                            selected = currentStyle == DarkThemeStyle.OLED,
                            onClick = { onStyleSelected(DarkThemeStyle.OLED) },
                            label = { Text("OLED") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
