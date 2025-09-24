package id.xms.xtrakernelmanager.ui.screens


import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import id.xms.xtrakernelmanager.ui.components.BatteryUsageCard
import id.xms.xtrakernelmanager.viewmodel.InfoViewModel

@Composable
fun InfoScreen(vm: InfoViewModel = hiltViewModel()) {
    val batteryStats by vm.batteryStats.collectAsState()
    val sotCached by vm.sotCached.collectAsState()

    LazyColumn {
        item {
            BasicText(
                text = "Information",
                style = MaterialTheme.typography.headlineMedium.copy(color = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }
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