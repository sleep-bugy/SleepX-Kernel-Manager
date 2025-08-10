package id.xms.xtrakernelmanager.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.xms.xtrakernelmanager.viewmodel.ThermalProfile
import id.xms.xtrakernelmanager.viewmodel.TuningViewModel

@Composable
fun ThermalCard(
    viewModel: TuningViewModel,
    blur: Boolean,
    modifier: Modifier = Modifier
) {
    val availableProfiles = viewModel.availableThermalProfiles
    val currentProfileName by viewModel.currentThermalProfileName.collectAsState()
    val currentProfileIndex by viewModel.currentThermalModeIndex.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(true) }

    GlassCard(blur, modifier) {
        Column(
            Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Thermal Profile",
                    style = MaterialTheme.typography.titleLarge
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    if (currentProfileName == "Loading...") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Loading profile...")
                        }
                    } else if (availableProfiles.isEmpty()) {
                        Text(
                            "No thermal profiles defined.",
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {

                        Text(
                            text = "Currently Thermal: $currentProfileName",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "Active Profile:",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = { showDialog = true },
                                modifier = Modifier.weight(1.5f)
                            ) {
                                Text(
                                    text = currentProfileName,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Change Profile",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (showDialog) {
                        ThermalProfileSelectionDialog(
                            availableProfiles = availableProfiles,
                            currentProfileIndex = currentProfileIndex,
                            onProfileSelected = { selectedProfile ->
                                viewModel.setThermalProfile(selectedProfile)
                                showDialog = false
                            },
                            onDismiss = { showDialog = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThermalProfileSelectionDialog(
    availableProfiles: List<ThermalProfile>,
    currentProfileIndex: Int,
    onProfileSelected: (ThermalProfile) -> Unit,
    onDismiss: () -> Unit,
) {
    if (availableProfiles.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Thermal Profile") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(availableProfiles) { profile ->
                    OutlinedButton(
                        onClick = { onProfileSelected(profile) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (profile.index == currentProfileIndex) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            contentColor = if (profile.index == currentProfileIndex) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(
                            text = profile.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
