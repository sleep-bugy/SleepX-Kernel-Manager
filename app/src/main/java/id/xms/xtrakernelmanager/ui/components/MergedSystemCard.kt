package id.xms.xtrakernelmanager.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.xms.xtrakernelmanager.data.model.BatteryInfo
import id.xms.xtrakernelmanager.data.model.DeepSleepInfo
import id.xms.xtrakernelmanager.data.model.MemoryInfo
import id.xms.xtrakernelmanager.data.model.SystemInfo

// Helper function to format time duration with seconds
private fun formatTimeWithSeconds(timeInMillis: Long): String {
    val totalSeconds = timeInMillis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

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
    // Main container with two cards and device info below
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top row with two cards side by side
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Left Card - Battery Information with Circular Progress
            BatteryCard(
                batteryInfo = b,
                blur = blur,
                modifier = Modifier.weight(1f)
            )

            // Right Card - RAM Information with Circular Progress
            RamCard(
                memoryInfo = mem,
                blur = blur,
                modifier = Modifier.weight(1f)
            )
        }

        // Bottom Card - Device and App Information
        DeviceInfoCard(
            systemInfo = systemInfo,
            rooted = rooted,
            version = version,
            blur = blur
        )
    }
}

@Composable
private fun BatteryCard(
    batteryInfo: BatteryInfo,
    blur: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    GlassCard(blur, modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Battery",
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

            // Circular Battery Progress Bar
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(120.dp)
            ) {
                CircularProgressIndicator(
                    progress = batteryInfo.level / 100f,
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 8.dp,
                    color = when {
                        batteryInfo.level > 60 -> MaterialTheme.colorScheme.primary
                        batteryInfo.level > 30 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${batteryInfo.level}%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    // Only show charging type if actually charging AND charging type is not empty
                    if (batteryInfo.isCharging && batteryInfo.chargingType.isNotEmpty() && batteryInfo.chargingType != "Not charging") {
                        Text(
                            text = batteryInfo.chargingType,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Basic Battery Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Convert and format voltage properly (from microvolts to volts)
                val formattedVoltage = if (batteryInfo.voltage > 0) {
                    val voltageInVolts = when {
                        batteryInfo.voltage > 1000000 -> batteryInfo.voltage / 1000000f // Convert from microvolts
                        batteryInfo.voltage > 1000 -> batteryInfo.voltage / 1000f // Convert from millivolts
                        else -> batteryInfo.voltage // Already in volts
                    }
                    String.format("%.2f", voltageInVolts).trimEnd('0').trimEnd('.')
                } else {
                    "0"
                }

                Text(
                    text = "${String.format("%.1f", batteryInfo.temp)}°C • ${formattedVoltage}V",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (batteryInfo.current != 0f) {
                    val currentMa = batteryInfo.current / 1000
                    val displayCurrent = kotlin.math.abs(currentMa)

                    Text(
                        text = "${String.format("%.0f", displayCurrent)}mA",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (batteryInfo.isCharging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
                if (batteryInfo.activeDrain != 0f) {
                    Text(
                        text = "Drain: ${String.format("%.1f", batteryInfo.activeDrain)}mAh/h",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Expanded Details
            AnimatedVisibility(visible = expanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider()

                    Text(
                        text = "Battery Details",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Health: ${batteryInfo.health}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Technology: ${batteryInfo.technology}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Status: ${if (batteryInfo.isCharging && batteryInfo.chargingType.isNotEmpty() && batteryInfo.chargingType != "Not charging") "Charging" else "Not Charging"}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    // Screen Time Information
                    if (batteryInfo.screenOnTime > 0L || batteryInfo.screenOffTime > 0L) {
                        Text(
                            text = "Screen Time",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (batteryInfo.screenOnTime > 0L) {
                            Text(
                                text = "Screen On: ${formatTimeWithSeconds(batteryInfo.screenOnTime)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (batteryInfo.screenOffTime > 0L) {
                            Text(
                                text = "Screen Off: ${formatTimeWithSeconds(batteryInfo.screenOffTime)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RamCard(
    memoryInfo: MemoryInfo,
    blur: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val usedPercentage = ((memoryInfo.used.toDouble() / memoryInfo.total.toDouble()) * 100).toInt()
    val freeRam = memoryInfo.total - memoryInfo.used

    GlassCard(blur, modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RAM",
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

            // Circular RAM Progress Bar
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(120.dp)
            ) {
                CircularProgressIndicator(
                    progress = usedPercentage / 100f,
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 8.dp,
                    color = when {
                        usedPercentage < 60 -> MaterialTheme.colorScheme.primary
                        usedPercentage < 80 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${usedPercentage}%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "used",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Basic RAM Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${memoryInfo.used / (1024 * 1024)}MB • ${memoryInfo.total / (1024 * 1024 * 1024)}GB Total",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Free: ${freeRam / (1024 * 1024)}MB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Expanded Details
            AnimatedVisibility(visible = expanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider()

                    Text(
                        text = "RAM Details",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Used: ${memoryInfo.used / (1024 * 1024)}MB",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Free: ${memoryInfo.free / (1024 * 1024)}MB",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Total: ${memoryInfo.total / (1024 * 1024)}MB",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceInfoCard(
    systemInfo: SystemInfo,
    rooted: Boolean,
    version: String,
    blur: Boolean,
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Device Info",
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
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = systemInfo.codename,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Expanded Details
            AnimatedVisibility(visible = expanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider()

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
                }
            }
        }
    }
}
