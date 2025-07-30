package id.xms.xtrakernelmanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import id.xms.xtrakernelmanager.ui.components.TuningClusterCard
import id.xms.xtrakernelmanager.ui.components.ThermalCard
import id.xms.xtrakernelmanager.viewmodel.TuningViewModel

@Composable
fun TuningScreen(vm: TuningViewModel = hiltViewModel()) {
    val clusters by remember { derivedStateOf { vm.clusters } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(clusters.size) { idx ->
            val cluster = clusters[idx]
            TuningClusterCard(cluster) { gov ->
                vm.applyCpuGov(cluster.name, gov)
            }
        }
        item { ThermalCard { vm.setThermal(it) } }
    }
}