package id.xms.xtrakernelmanager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DiscFull
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.xms.xtrakernelmanager.viewmodel.TuningViewModel

@Composable
fun IoSchedulerCard(
    vm: TuningViewModel,
    modifier: Modifier = Modifier,
    blur: Boolean
) {
    val availableSchedulers by vm.availableIoSchedulers.collectAsState()
    val currentScheduler by vm.currentIoScheduler.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    val isLoading = availableSchedulers.isEmpty() && currentScheduler == "Loading..."

    SuperGlassCard(
        modifier = modifier.fillMaxWidth(),
        glassIntensity = if (blur) GlassIntensity.Light else GlassIntensity.Light
    ) {
        Column { // Hapus padding dari sini jika CompactControlItem sudah punya
            CompactControlItem(
                title = "I/O Scheduler",
                icon = Icons.Default.DiscFull,
                value = if (isLoading) "Loading..." else currentScheduler,
                isLoading = isLoading,
                themeColor = MaterialTheme.colorScheme.secondary,
                enabled = !isLoading && availableSchedulers.isNotEmpty(),
                onClick = { showDialog = true }
            )
        }
    }

    if (showDialog) {
        IoSchedulerDialog(
            availableSchedulers = availableSchedulers,
            currentScheduler = currentScheduler,
            onSchedulerSelected = { scheduler ->
                vm.setIoScheduler(scheduler)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun IoSchedulerDialog(
    availableSchedulers: List<String>,
    currentScheduler: String,
    onSchedulerSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Select I/O Scheduler", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                availableSchedulers.forEach { scheduler ->
                    val isSelected = scheduler == currentScheduler
                    OutlinedButton(
                        onClick = { onSchedulerSelected(scheduler) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = if (isSelected) {
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        } else {
                            ButtonDefaults.outlinedButtonColors()
                        }
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = scheduler,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    )
}