package id.xms.xtrakernelmanager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.xms.xtrakernelmanager.data.model.BatteryInfo

@Composable
fun BatteryCard(info: BatteryInfo) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Battery", style = MaterialTheme.typography.titleMedium)
            Text("Level: ${info.level}% | Temp: ${info.temp}°C")
            Text("Health: ${info.health}% | Cycles: ${info.cycles}")
        }
    }
}