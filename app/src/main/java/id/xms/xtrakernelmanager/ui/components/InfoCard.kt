package id.xms.xtrakernelmanager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.xms.xtrakernelmanager.data.model.KernelInfo

@Composable
fun InfoCard(kernel: KernelInfo) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Kernel", style = MaterialTheme.typography.titleMedium)
            Text("Version: ${kernel.version}")
            Text("GKI: ${kernel.gkiType}")
            Text("Scheduler: ${kernel.scheduler}")
        }
    }
}