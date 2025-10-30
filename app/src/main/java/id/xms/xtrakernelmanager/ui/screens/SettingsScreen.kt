package id.xms.xtrakernelmanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import id.xms.xtrakernelmanager.R
import id.xms.xtrakernelmanager.ui.viewmodel.SettingsViewModel
import id.xms.xtrakernelmanager.ui.viewmodel.ThemeViewModel
import id.xms.xtrakernelmanager.data.model.ThemeType
import id.xms.xtrakernelmanager.util.Language

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val currentTheme by themeViewModel.currentTheme.collectAsState()
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Theme Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = { showThemeDialog = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Theme",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = currentTheme.displayName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Language Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = { showLanguageDialog = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.language),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = currentLanguage.displayName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Donate Card
        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
        val donateLink = stringResource(id = R.string.donate_link)
        val donateText = stringResource(id = R.string.donate)
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = { uriHandler.openUri(donateLink) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = donateText,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Support development",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AssistChip(
                    onClick = { uriHandler.openUri(donateLink) },
                    label = { Text(donateText) }
                )
            }
        }
    }

    // Theme Selection Dialog
    if (showThemeDialog) {
        val filteredThemes = remember {
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                ThemeType.entries.filter { it != ThemeType.GLASSMORPHISM }
            } else {
                ThemeType.entries
            }
        }
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Select Theme") },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            },
            text = {
                Column {
                    filteredThemes.forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(theme.displayName)
                            RadioButton(
                                selected = currentTheme == theme,
                                onClick = {
                                    themeViewModel.setTheme(theme)
                                    showThemeDialog = false
                                }
                            )
                        }
                    }
                }
            }
        )
    }

    // Language Selection Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.select_language)) },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            },
            text = {
                Column {
                    Language.entries.forEach { language ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(language.displayName)
                            RadioButton(
                                selected = currentLanguage == language,
                                onClick = {
                                    viewModel.setLanguage(language)
                                    showLanguageDialog = false
                                }
                            )
                        }
                    }
                }
            }
        )
    }
}
