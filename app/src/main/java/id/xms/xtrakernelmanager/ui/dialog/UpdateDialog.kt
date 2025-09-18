package id.xms.xtrakernelmanager.ui.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun UpdateDialog(
    version: String,
    changelog: String,
    url: String,
    force: Boolean = false,
    onDismiss: () -> Unit,
    onUpdateClick: () -> Unit
) {
    Dialog(onDismissRequest = { if (!force) onDismiss() }) {
        Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 8.dp) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Update Available", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Version: $version", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(changelog, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (!force) {
                        TextButton(onClick = onDismiss) { Text("Later") }
                    }
                    Button(onClick = onUpdateClick) { Text("Update") }
                }
            }
        }
    }
}

