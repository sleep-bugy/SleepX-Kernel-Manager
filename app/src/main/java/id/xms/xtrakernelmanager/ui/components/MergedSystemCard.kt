package id.xms.xtrakernelmanager.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.xms.xtrakernelmanager.data.model.BatteryInfo
import id.xms.xtrakernelmanager.data.model.DeepSleepInfo
import id.xms.xtrakernelmanager.data.model.MemoryInfo
import id.xms.xtrakernelmanager.data.model.SystemInfo
import java.util.Locale

// Helper function to format time duration with seconds
private fun formatTimeWithSeconds(timeInMillis: Long): String {
    val totalSeconds = timeInMillis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
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
    // Main container with cards
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Combined Battery & RAM Card
        BatteryRamCard(
            batteryInfo = b,
            memoryInfo = mem,
            blur = blur
        )

        // Device Information Card
        DeviceInfoCard(
            systemInfo = systemInfo,
            rooted = rooted,
            version = version,
            blur = blur
        )
    }
}

@Composable
private fun BatteryRamCard(
    batteryInfo: BatteryInfo,
    memoryInfo: MemoryInfo,
    blur: Boolean,
    modifier: Modifier = Modifier
) {
    val usedPercentage = ((memoryInfo.used.toDouble() / memoryInfo.total.toDouble()) * 100).toInt()

    // Animation untuk pulse effect
    val infiniteTransition = rememberInfiniteTransition(label = "system_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    SuperGlassCard(
        modifier = modifier,
        glassIntensity = GlassIntensity.Light,
        onClick = null
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Section
            SystemHeaderSection(batteryInfo = batteryInfo, memoryInfo = memoryInfo, usedPercentage = usedPercentage, pulseAlpha = pulseAlpha)

            // Progress Bars Section
            SystemProgressSection(batteryInfo = batteryInfo, memoryInfo = memoryInfo, usedPercentage = usedPercentage)

            // Stats Section
            SystemStatsSection(batteryInfo = batteryInfo, memoryInfo = memoryInfo)
        }
    }
}

@Composable
private fun SystemHeaderSection(
    batteryInfo: BatteryInfo,
    memoryInfo: MemoryInfo,
    usedPercentage: Int,
    pulseAlpha: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "System Status",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Battery Status Box
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                )
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${batteryInfo.level}% Battery",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                    )
                }

                // RAM Status Box
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f),
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                )
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${usedPercentage}% RAM",
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                    )
                }
            }
        }

        // Animated System icons with pulse effect
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Battery Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF4CAF50).copy(alpha = pulseAlpha * 0.6f),
                                    Color.Transparent
                                ),
                                radius = size.minDimension * 0.8f
                            ),
                            radius = size.minDimension * 0.5f
                        )
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BatteryFull,
                    contentDescription = "Battery",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Memory Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF2196F3).copy(alpha = pulseAlpha * 0.6f),
                                    Color.Transparent
                                ),
                                radius = size.minDimension * 0.8f
                            ),
                            radius = size.minDimension * 0.5f
                        )
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = "Memory",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun SystemProgressSection(
    batteryInfo: BatteryInfo,
    memoryInfo: MemoryInfo,
    usedPercentage: Int
) {
    GlassmorphismSurface(
        modifier = Modifier.fillMaxWidth(),
        blurRadius = 0f,
        alpha = 0.4f
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(20.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Battery Progress Bar
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BatteryFull,
                                contentDescription = "Battery",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Battery",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${batteryInfo.level}%",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Battery Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(batteryInfo.level / 100f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            when {
                                                batteryInfo.level > 70 -> Color(0xFF4CAF50)
                                                batteryInfo.level > 30 -> Color(0xFFFF9800)
                                                else -> Color(0xFFF44336)
                                            },
                                            when {
                                                batteryInfo.level > 70 -> Color(0xFF66BB6A)
                                                batteryInfo.level > 30 -> Color(0xFFFFB74D)
                                                else -> Color(0xFFEF5350)
                                            }
                                        )
                                    )
                                )
                        )
                    }
                }

                // RAM Progress Bar
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = "Memory",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Memory",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${usedPercentage}%",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }

                    // RAM Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(usedPercentage / 100f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            when {
                                                usedPercentage < 60 -> Color(0xFF2196F3)
                                                usedPercentage < 80 -> Color(0xFFFF9800)
                                                else -> Color(0xFFF44336)
                                            },
                                            when {
                                                usedPercentage < 60 -> Color(0xFF42A5F5)
                                                usedPercentage < 80 -> Color(0xFFFFB74D)
                                                else -> Color(0xFFEF5350)
                                            }
                                        )
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemStatsSection(
    batteryInfo: BatteryInfo,
    memoryInfo: MemoryInfo
) {
    val freeRam = memoryInfo.total - memoryInfo.used

    GlassmorphismSurface(
        modifier = Modifier.fillMaxWidth(),
        blurRadius = 0f,
        alpha = 0.4f
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Battery Stats Row 1 - Temperature and Voltage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Temperature
                    SystemStatItem(
                        icon = Icons.Default.Thermostat,
                        label = "Temp",
                        value = "${String.format(Locale.getDefault(), "%.1f", batteryInfo.temp)}°C",
                        color = when {
                            batteryInfo.temp > 40 -> MaterialTheme.colorScheme.error
                            batteryInfo.temp > 35 -> Color(0xFFFF9800)
                            else -> MaterialTheme.colorScheme.tertiary
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // Voltage
                    SystemStatItem(
                        icon = Icons.Default.ElectricBolt,
                        label = "Voltage",
                        value = run {
                            val formattedVoltage = if (batteryInfo.voltage > 0) {
                                val voltageInVolts = when {
                                    batteryInfo.voltage > 1000000 -> batteryInfo.voltage / 1000000f
                                    batteryInfo.voltage > 1000 -> batteryInfo.voltage / 1000f
                                    else -> batteryInfo.voltage
                                }
                                String.format(Locale.getDefault(), "%.2f", voltageInVolts).trimEnd('0').trimEnd('.')
                            } else "0"
                            "${formattedVoltage}V"
                        },
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Battery Stats Row 2 - Health and Cycles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Battery Health
                    SystemStatItem(
                        icon = Icons.Default.HealthAndSafety,
                        label = "Health",
                        value = if (batteryInfo.healthPercentage > 0) "${batteryInfo.healthPercentage}%" else batteryInfo.health,
                        color = when {
                            batteryInfo.healthPercentage >= 80 -> Color(0xFF4CAF50)
                            batteryInfo.healthPercentage >= 60 -> Color(0xFFFF9800)
                            batteryInfo.healthPercentage > 0 -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // Battery Cycles
                    SystemStatItem(
                        icon = Icons.Default.Autorenew,
                        label = "Cycles",
                        value = if (batteryInfo.cycleCount > 0) "${batteryInfo.cycleCount}" else "N/A",
                        color = when {
                            batteryInfo.cycleCount <= 300 -> Color(0xFF4CAF50)
                            batteryInfo.cycleCount <= 500 -> Color(0xFFFF9800)
                            batteryInfo.cycleCount > 500 -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Battery Stats Row 3 - Capacity
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Design Capacity
                    SystemStatItem(
                        icon = Icons.Default.BatterySaver,
                        label = "Capacity",
                        value = if (batteryInfo.capacity > 0) "${batteryInfo.capacity}mAh" else "N/A",
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )

                    // Current Capacity
                    SystemStatItem(
                        icon = Icons.Default.Battery6Bar,
                        label = "Current Cap",
                        value = if (batteryInfo.currentCapacity > 0) "${batteryInfo.currentCapacity}mAh" else "N/A",
                        color = when {
                            batteryInfo.currentCapacity >= (batteryInfo.capacity * 0.8f) -> Color(0xFF4CAF50)
                            batteryInfo.currentCapacity >= (batteryInfo.capacity * 0.6f) -> Color(0xFFFF9800)
                            batteryInfo.currentCapacity > 0 -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // RAM Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Used RAM
                    SystemStatItem(
                        icon = Icons.Default.Memory,
                        label = "Used RAM",
                        value = "${memoryInfo.used / (1024 * 1024)}MB",
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )

                    // Free RAM
                    SystemStatItem(
                        icon = Icons.Default.Storage,
                        label = "Free RAM",
                        value = "${freeRam / (1024 * 1024)}MB",
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Additional info if current is available
                if (batteryInfo.current != 0f) {
                    val currentMa = batteryInfo.current / 1000
                    val displayCurrent = kotlin.math.abs(currentMa)
                    SystemStatItem(
                        icon = if (batteryInfo.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryAlert,
                        label = "Current",
                        value = "${String.format(Locale.getDefault(), "%.0f", displayCurrent)}mA",
                        color = if (batteryInfo.isCharging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun SystemStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = color
            )
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

    SuperGlassCard(
        modifier = modifier,
        glassIntensity = GlassIntensity.Light,
        onClick = { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Section
            DeviceHeaderSection(expanded = expanded, onToggle = { expanded = !expanded })

            // Main Device Display
            DeviceMainSection(systemInfo = systemInfo, rooted = rooted, version = version)

            // Expanded Details
            AnimatedVisibility(visible = expanded) {
                DeviceDetailsSection(systemInfo = systemInfo, rooted = rooted, version = version)
            }
        }
    }
}

@Composable
private fun DeviceHeaderSection(
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Smartphone,
                contentDescription = "Device",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Device Info",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        IconButton(
            onClick = onToggle,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Show less" else "Show more",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DeviceMainSection(
    systemInfo: SystemInfo,
    rooted: Boolean,
    version: String
) {
    GlassmorphismSurface(
        modifier = Modifier.fillMaxWidth(),
        blurRadius = 0f,
        alpha = 0.4f
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Device Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = systemInfo.model,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = systemInfo.codename,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Android ${systemInfo.androidVersion}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Status Icons
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Root Status
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (rooted) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.errorContainer
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (rooted) "ROOT" else "NO ROOT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (rooted) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onErrorContainer
                        )
                    }

                    // App Version
                    Text(
                        text = "XKM v$version",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceDetailsSection(
    systemInfo: SystemInfo,
    rooted: Boolean,
    version: String
) {
    GlassmorphismSurface(
        modifier = Modifier.fillMaxWidth(),
        blurRadius = 0f,
        alpha = 0.4f
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "System Details",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // SoC Information
                DeviceDetailSection(
                    icon = Icons.Default.Memory,
                    title = "System on Chip",
                    details = listOf(
                        "SoC" to systemInfo.soc,
                        "Model" to systemInfo.model
                    )
                )

                // System Information
                DeviceDetailSection(
                    icon = Icons.Default.Android,
                    title = "Operating System",
                    details = listOf(
                        "Android Version" to systemInfo.androidVersion,
                        "API Level" to systemInfo.sdk.toString(),
                        "Fingerprint" to systemInfo.fingerprint
                    )
                )

                // App Information
                DeviceDetailSection(
                    icon = Icons.Default.Settings,
                    title = "Application",
                    details = listOf(
                        "Version" to "XKM v$version",
                        "Root Access" to if (rooted) "Available" else "Not Available"
                    )
                )
            }
        }
    }
}

@Composable
private fun DeviceDetailSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    details: List<Pair<String, String>>
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }

        details.forEach { (label, value) ->
            DeviceDetailRow(label = label, value = value)
        }
    }
}

@Composable
private fun DeviceDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
            maxLines = if (label == "Fingerprint") 2 else 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
