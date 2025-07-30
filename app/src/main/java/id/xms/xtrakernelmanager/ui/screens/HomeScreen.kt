package id.xms.xtrakernelmanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
// import androidx.compose.ui.graphics.Brush // Tidak digunakan secara langsung di file ini
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import id.xms.xtrakernelmanager.data.model.GpuInfo
import id.xms.xtrakernelmanager.ui.components.*
import id.xms.xtrakernelmanager.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: HomeViewModel = hiltViewModel()) {
    val cpuInfo by vm.cpuInfo.collectAsState()
    val battery = vm.batteryInfo
    val memory = vm.memoryInfo
    val deepSleep by vm.deepSleep.collectAsState()
    val root = vm.rootStatus
    val kernel = vm.kernelInfo
    val version = vm.appVersion
    var blurOn by rememberSaveable { mutableStateOf(true) }

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