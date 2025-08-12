package id.xms.xtrakernelmanager.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.xms.xtrakernelmanager.data.model.BatteryInfo
import id.xms.xtrakernelmanager.data.model.DeepSleepInfo
import id.xms.xtrakernelmanager.data.model.MemoryInfo
import id.xms.xtrakernelmanager.data.model.SystemInfo
import id.xms.xtrakernelmanager.R

@Composable
fun MergedSystemCard(
    b: BatteryInfo,
    d: DeepSleepInfo,
    rooted: Boolean,
    version: String,
    blur: Boolean,
    mem: MemoryInfo,
    systemInfo: SystemInfo,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    GlassCard(blur, modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("System Info", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (!expanded){
                        Badge { Text(if (rooted) "Phone Is Rooted" else "Unrooted Phone", style = MaterialTheme.typography.labelMedium) }
                    }
                }
                Icon(
                    imageVector = if (expanded) Filled.KeyboardArrowUp else Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }
            Spacer(modifier = Modifier.height(8.dp)) // Added space between System Info title and Device Details

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { // Device Details
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Smartphone, contentDescription = "Device Details Icon", modifier = Modifier.size(24.dp))
                        Text("Device Details", style = MaterialTheme.typography.titleMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Filled.PhoneAndroid, contentDescription = "Model Icon", modifier = Modifier.size(18.dp))
                            Text("Model:")
                        }
                        Text(systemInfo.model)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Filled.Code, contentDescription = "Codename Icon", modifier = Modifier.size(18.dp))
                            Text("Codename:")
                        }
                        Text(systemInfo.codename)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Filled.Android, contentDescription = "Android Version Icon", modifier = Modifier.size(18.dp))
                            Text("Android Version:")
                        }
                        Text(systemInfo.androidVersion)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Filled.Layers, contentDescription = "SDK Level Icon", modifier = Modifier.size(18.dp))
                            Text("SDK Level:")
                        }
                        Text(systemInfo.sdk.toString())
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Filled.Fingerprint, contentDescription = "Fingerprint Icon", modifier = Modifier.size(18.dp))
                            Text("Fingerprint:")
                        }
                    }
                    Text(
                        systemInfo.fingerprint,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Divider(Modifier.padding(vertical = 8.dp))


                    // Battery Status
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.BatteryFull, contentDescription = "Battery Icon", modifier = Modifier.size(24.dp))
                        Text("Battery & Power", style = MaterialTheme.typography.titleMedium)
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Level:"); Text(if (b.level >= 0) "${b.level}%" else "Unknown")
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Temp:"); Text("${"%.1f".format(b.temp)}°C")
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Health:"); Text(if (b.health >= 0) "${b.health}%" else "Unknown")
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Cycles:"); Text(if (b.cycles >= 0) "${b.cycles}" else "Unknown")
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Capacity:"); Text(if (b.capacity >= 0) "${b.capacity} mAH" else "Unknown")
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        val uptimeSeconds = d.uptime / 1000
                        val uptimeHours = uptimeSeconds / 3600
                        val uptimeMinutes = (uptimeSeconds % 3600) / 60
                        val uptimeSecs = uptimeSeconds % 60
                        Text("Uptime:"); Text(String.format(stringResource(R.string.uptime_regex), uptimeHours, uptimeMinutes, uptimeSecs))
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        val deepSleepSeconds = d.deepSleep / 1000
                        val deepSleepHours = deepSleepSeconds / 3600
                        val deepSleepMinutes = (deepSleepSeconds % 3600) / 60
                        val deepSleepSecs = deepSleepSeconds % 60
                        Text("Deep Sleep:"); Text(
                            String.format("%02dh %02dm %02ds", deepSleepHours, deepSleepMinutes, deepSleepSecs)
                        )
                    }

                    Divider(Modifier.padding(vertical = 8.dp))

                    // RAM Usage
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Memory, contentDescription = "RAM Icon", modifier = Modifier.size(24.dp))
                        Text("RAM Usage", style = MaterialTheme.typography.titleMedium)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val totalMem = if (mem.total > 0) mem.total else 1L
                        val usedPercentage = (mem.used.toFloat() / totalMem.toFloat() * 100).toInt()
                        val freePercentage = ((totalMem - mem.used).toFloat() / totalMem.toFloat() * 100).toInt()

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Used: ${mem.used / 1_000_000} MB", style = MaterialTheme.typography.labelSmall)
                            Badge(
                                containerColor = when {
                                    usedPercentage > 75 -> MaterialTheme.colorScheme.errorContainer
                                    usedPercentage > 50 -> MaterialTheme.colorScheme.tertiaryContainer
                                    else -> MaterialTheme.colorScheme.primaryContainer
                                }
                            ) { Text("$usedPercentage%", style = MaterialTheme.typography.labelSmall) }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Free: ${(totalMem - mem.used) / 1_000_000} MB", style = MaterialTheme.typography.labelSmall)
                            Badge(
                                containerColor = when {
                                    freePercentage < 25 -> MaterialTheme.colorScheme.errorContainer
                                    freePercentage < 50 -> MaterialTheme.colorScheme.tertiaryContainer
                                    else -> MaterialTheme.colorScheme.primaryContainer
                                }
                            ) { Text("$freePercentage%", style = MaterialTheme.typography.labelSmall) }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Total: ${totalMem / 1_000_000} MB", style = MaterialTheme.typography.labelSmall)
                            Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) { Text("100%", style = MaterialTheme.typography.labelSmall) }
                        }
                    }

                    Divider(Modifier.padding(vertical = 8.dp))

                    // Root & Version
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Info, contentDescription = "App Info Icon", modifier = Modifier.size(24.dp))
                        Text("App Info", style = MaterialTheme.typography.titleMedium)
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Root Status:"); Text(if (rooted) "Granted" else "Not Granted")
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("App Version:"); Text(version)
                    }
                }
            }
        }
    }
}

