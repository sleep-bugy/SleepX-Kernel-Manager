package id.xms.xtrakernelmanager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.xms.xtrakernelmanager.viewmodel.TuningViewModel

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

    SuperGlassCard(
        modifier = modifier.fillMaxWidth(),
        glassIntensity = if (blur) GlassIntensity.Light else GlassIntensity.Light
    ) {
        Column {
            CompactControlItem(
                title = "TCP Congestion Control",
                icon = Icons.Default.NetworkCheck,
                value = if (isLoading) "Loading..." else currentAlgorithm,
                isLoading = isLoading,
                themeColor = MaterialTheme.colorScheme.tertiary,
                enabled = !isLoading && availableAlgorithms.isNotEmpty(),
                onClick = { showDialog = true }
            )
        }
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
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Select TCP Algorithm", fontWeight = FontWeight.Bold) },
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
