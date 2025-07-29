package id.xms.xtrakernelmanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import id.xms.xtrakernelmanager.data.model.GpuInfo
import id.xms.xtrakernelmanager.ui.components.*
import id.xms.xtrakernelmanager.viewmodel.HomeViewModel

@Composable
fun HomeScreen(vm: HomeViewModel = hiltViewModel()) {
    val clusters by vm.clusters.collectAsState()
    val battery by vm.battery.collectAsState()
    val system by vm.system.collectAsState()
    val gpu by vm.gpu.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(clusters) { cluster ->
            CpuClusterCard(cluster)
        }
        gpu?.let { gpuInfo ->
            item { GpuCard(gpuInfo) }
        }
        battery?.let { bat ->
            item { BatteryCard(bat) }
        }
        system?.let { sys ->
            item { SystemCard(sys) }
        }
    }
}

/* -------------- PLACEHOLDER agar tidak merah -------------- */
@Composable
fun GpuCard(info: GpuInfo) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("GPU", style = MaterialTheme.typography.titleMedium)
            Text("Renderer: ${info.renderer}")
            Text("OpenGL ES: ${info.glEsVersion}")
        }
    }
}