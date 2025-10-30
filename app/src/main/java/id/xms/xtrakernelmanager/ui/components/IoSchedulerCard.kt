package id.xms.xtrakernelmanager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DiscFull
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.xms.xtrakernelmanager.viewmodel.TuningViewModel
import id.xms.xtrakernelmanager.R

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

    Column(modifier = modifier.fillMaxWidth()) {
        ListItem(
            leadingContent = { Icon(Icons.Default.DiscFull, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
            headlineContent = { Text(stringResource(id = R.string.io_scheduler_label)) },
            supportingContent = {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text(currentScheduler, color = MaterialTheme.colorScheme.secondary)
            },
            trailingContent = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.clickable(enabled = !isLoading && availableSchedulers.isNotEmpty()) { showDialog = true }
        )
        HorizontalDivider()
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
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.action_cancel)) }
        },
        title = { Text(stringResource(id = R.string.title_select_io), fontWeight = FontWeight.Bold) },
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
