package id.xms.xtrakernelmanager.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
                Text(
                    text = stringResource(R.string.system_info),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Show less" else "Show more"
                    )
                }
            }

            // Battery Information Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.BatteryFull,
                    contentDescription = "Battery",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "${b.level}% ${if (b.isCharging) "(${b.chargingType})" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${String.format("%.1f", b.temp)}°C · ${String.format("%.1f", b.voltage)}V",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (b.current != 0f) {
                        Text(
                            text = "${String.format("%.0f", b.current / 1000)}mA",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (b.current > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Memory Information
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Memory,
                    contentDescription = "Memory",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${((mem.used.toDouble() / mem.total.toDouble()) * 100).toInt()}% used of ${mem.total / (1024 * 1024 * 1024)}GB",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Battery Health Information
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BatteryFull,
                            contentDescription = "Battery Health",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Battery Health: ${b.health}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Technology: ${b.technology} • Status: ${b.status}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (b.current != 0f) {
                                Text(
                                    text = "Current: ${String.format("%.0f", b.current)}mA",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (b.chargingWattage != 0f) {
                                Text(
                                    text = "Charging: ${String.format("%.1f", b.chargingWattage)}W",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    // App Information
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "App Info",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "XKM v$version",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Root: ${if (rooted) "Granted" else "Not Available"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (rooted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    // Device Information
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Smartphone,
                            contentDescription = "Device",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = systemInfo.model,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = systemInfo.codename,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // SoC Information
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = "SoC",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = systemInfo.soc,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Android ${systemInfo.androidVersion} (API ${systemInfo.sdk})",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
