package id.xms.xtrakernelmanager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.xms.xtrakernelmanager.data.model.RealtimeCpuInfo

@Composable
fun CpuCard(
    info: RealtimeCpuInfo,
    blur: Boolean,
    modifier: Modifier = Modifier
) {
    GlassCard(blur, modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Central Proccessing Unit (CPU)", style = MaterialTheme.typography.titleLarge)
            Text("${info.freqs.joinToString { "$it MHz" }}")
            Text("CPU Cores: ${info.cores} Core")
            Text("CPU Governor: ${info.governor}")
            Text("Temperature: ${"%.1f".format(info.temp)}°C")
        }
    }
}