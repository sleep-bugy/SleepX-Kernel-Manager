package id.xms.xtrakernelmanager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NetworkCheck
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
fun TcpCard(
    vm: TuningViewModel,
    modifier: Modifier = Modifier,
    blur: Boolean
) {
    val availableAlgorithms by vm.availableTcpAlgorithms.collectAsState()
    val currentAlgorithm by vm.currentTcpAlgorithm.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    val isLoading = availableAlgorithms.isEmpty() && currentAlgorithm == "Loading..."

    Column(modifier = modifier.fillMaxWidth()) {
        ListItem(
            leadingContent = { Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) },
            headlineContent = { Text(stringResource(id = R.string.tcp_congestion_label)) },
            supportingContent = {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text(currentAlgorithm, color = MaterialTheme.colorScheme.tertiary)
            },
            trailingContent = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.clickable(enabled = !isLoading && availableAlgorithms.isNotEmpty()) { showDialog = true }
        )
        HorizontalDivider()
    }

    if (showDialog) {
        TcpDialog(
            availableAlgorithms = availableAlgorithms,
            currentAlgorithm = currentAlgorithm,
            onAlgorithmSelected = { algorithm ->
                vm.setTcpAlgorithm(algorithm)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun TcpDialog(
    availableAlgorithms: List<String>,
    currentAlgorithm: String,
    onAlgorithmSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.action_cancel)) }
        },
        title = { Text(stringResource(id = R.string.title_select_tcp), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                availableAlgorithms.forEach { algorithm ->
                    val isSelected = algorithm == currentAlgorithm
                    OutlinedButton(
                        onClick = { onAlgorithmSelected(algorithm) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = if (isSelected) {
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
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
                            text = algorithm,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    )
}
