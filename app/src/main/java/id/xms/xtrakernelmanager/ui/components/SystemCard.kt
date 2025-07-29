package id.xms.xtrakernelmanager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.xms.xtrakernelmanager.data.model.SystemInfo

@Composable
fun SystemCard(info: SystemInfo) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("System", style = MaterialTheme.typography.titleMedium)
            Text("Model: ${info.model} (${info.codename})")
            Text("Android ${info.androidVersion} (SDK ${info.sdk})")
        }
    }
}