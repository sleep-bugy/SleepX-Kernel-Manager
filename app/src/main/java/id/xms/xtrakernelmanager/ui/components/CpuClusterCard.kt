package id.xms.xtrakernelmanager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.xms.xtrakernelmanager.data.model.CpuCluster

@Composable
fun CpuClusterCard(cluster: CpuCluster) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(cluster.name, style = MaterialTheme.typography.titleMedium)
            Text("Governor: ${cluster.governor}")
            Text("Freq: ${cluster.minFreq / 1000} – ${cluster.maxFreq / 1000} MHz")
        }
    }
}