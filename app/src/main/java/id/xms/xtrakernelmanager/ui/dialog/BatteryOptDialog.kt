package id.xms.xtrakernelmanager.ui.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun BatteryOptDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onExit: () -> Unit,
    showExitButton: Boolean = false
) {
    AlertDialog(
        onDismissRequest = { /* Prevent dismissal by tapping outside */ },
        title = { Text(if (showExitButton) "Permissions Required" else "Battery Optimization") },
        text = {
            Text(
                if (showExitButton) {
                    "XKM requires these permissions to function properly. Without them, the app cannot " +
                    "maintain your settings. Please grant the permissions or exit the app."
                } else {
                    "XKM needs to be excluded from battery optimization and requires notification " +
                    "permission to maintain your thermal settings in the background. Please allow " +
                    "these permissions for the app to work properly."
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Open Settings")
            }
        },
        dismissButton = {
            if (showExitButton) {
                TextButton(onClick = onExit) {
                    Text("Exit App")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Later")
                }
            }
        }
    )
}
