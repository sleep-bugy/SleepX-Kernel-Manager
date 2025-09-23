package id.xms.xtrakernelmanager.ui.screens


import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import id.xms.xtrakernelmanager.ui.components.BatteryUsageCard
import id.xms.xtrakernelmanager.viewmodel.InfoViewModel

@Composable
fun InfoScreen(vm: InfoViewModel = hiltViewModel()) {
    val batteryStats by vm.batteryStats.collectAsState()
    val sotCached by vm.sotCached.collectAsState()

    LazyColumn(/* ... */) {
        batteryStats?.let { stats ->
            item {
                BatteryUsageCard(
                    batteryStats = stats,
                    sotCached = sotCached,
                    onRefreshScreenOnTime = { vm.refreshScreenOnTime() }
                )
            }
        }
    }
}