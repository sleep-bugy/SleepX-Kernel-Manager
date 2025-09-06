package id.xms.xtrakernelmanager.ui.components

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.xms.xtrakernelmanager.data.model.BatteryInfo
import id.xms.xtrakernelmanager.data.model.DeepSleepInfo
import id.xms.xtrakernelmanager.data.model.MemoryInfo
import id.xms.xtrakernelmanager.data.model.StorageInfo
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

// Helper function to format storage size
private fun formatStorageSize(bytes: Long): String {
    val tb = 1024L * 1024L * 1024L * 1024L
    val gb = 1024L * 1024L * 1024L
    val mb = 1024L * 1024L
    val kb = 1024L

    return when {
        bytes >= tb -> String.format(Locale.getDefault(), "%.1f TB", bytes.toDouble() / tb)
        bytes >= gb -> String.format(Locale.getDefault(), "%.1f GB", bytes.toDouble() / gb)
        bytes >= mb -> String.format(Locale.getDefault(), "%.1f MB", bytes.toDouble() / mb)
        bytes >= kb -> String.format(Locale.getDefault(), "%.1f KB", bytes.toDouble() / kb)
        else -> "$bytes B"
    }
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
    storageInfo: StorageInfo,
    modifier: Modifier = Modifier
) {
    // Main container with separated cards
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Separate Battery Card
        BatteryCard(
            batteryInfo = b,
            blur = blur
        )

        // Separate Memory Card
        MemoryCard(
            memoryInfo = mem,
            blur = blur
        )

        // Separate Storage Card - reads actual device storage capacity
        StorageCard(
            storageInfo = storageInfo,
            blur = blur
        )

        // Device Information Card
        DeviceInfoCard(
            systemInfo = systemInfo,
            rooted = rooted,
            version = version,
            blur = blur,
            storageInfo = storageInfo
        )
    }
}

@Composable
private fun BatteryCard(
    batteryInfo: BatteryInfo,
    blur: Boolean,
    modifier: Modifier = Modifier
) {
    // Animation untuk pulse effect
    val infiniteTransition = rememberInfiniteTransition(label = "battery_pulse")
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
            // Battery Header Section
            BatteryHeaderSection(batteryInfo = batteryInfo, pulseAlpha = pulseAlpha)

            // Battery Progress Section
            BatteryProgressSection(batteryInfo = batteryInfo)

            // Battery Stats Section
            BatteryStatsSection(batteryInfo = batteryInfo)
        }
    }
}

@Composable
private fun MemoryCard(
    memoryInfo: MemoryInfo,
    blur: Boolean,
    modifier: Modifier = Modifier
) {
    val usedPercentage = ((memoryInfo.used.toDouble() / memoryInfo.total.toDouble()) * 100).toInt()

    // Animation untuk pulse effect
    val infiniteTransition = rememberInfiniteTransition(label = "memory_pulse")
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
            // Memory Header Section
            MemoryHeaderSection(memoryInfo = memoryInfo, usedPercentage = usedPercentage, pulseAlpha = pulseAlpha)

            // Memory Progress Section
            MemoryProgressSection(memoryInfo = memoryInfo, usedPercentage = usedPercentage)

            // Memory Stats Section
            MemoryStatsSection(memoryInfo = memoryInfo)
        }
    }
}

@Composable
private fun StorageCard(
    storageInfo: StorageInfo,
    blur: Boolean,
    modifier: Modifier = Modifier
) {
    val usedPercentage = ((storageInfo.usedSpace.toDouble() / storageInfo.totalSpace.toDouble()) * 100).toInt()

    // Animation untuk pulse effect
    val infiniteTransition = rememberInfiniteTransition(label = "storage_pulse")
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
            // Storage Header Section
            StorageHeaderSection(storageInfo = storageInfo, usedPercentage = usedPercentage, pulseAlpha = pulseAlpha)

            // Storage Progress Section
            StorageProgressSection(storageInfo = storageInfo)

            // Storage Stats Section
            StorageStatsSection(storageInfo = storageInfo)
        }
    }
}

@Composable
private fun BatteryHeaderSection(
    batteryInfo: BatteryInfo,
    pulseAlpha: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Battery Status",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

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
                    text = "${batteryInfo.level}% • ${if (batteryInfo.isCharging) "Charging" else "Discharging"}",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                )
            }
        }

        // Animated Battery Icon with pulse effect
        Box(
            modifier = Modifier
                .size(56.dp)
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
                imageVector = if (batteryInfo.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                contentDescription = "Battery",
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun MemoryHeaderSection(
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
                text = "Memory Status",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Memory Status Box
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
                val totalGb = (memoryInfo.total / (1024 * 1024 * 1024))
                val zramGb = (memoryInfo.zramTotal / (1024 * 1024 * 1024))
                val swapGb = (memoryInfo.swapTotal / (1024 * 1024 * 1024))

                val memoryText = buildString {
                    append("${usedPercentage}% Used • ${totalGb}GB")
                    if (zramGb > 0) append(" + ${zramGb}GB Zram")
                    if (swapGb > 0) append(" + ${swapGb}GB Swap")
                }

                Text(
                    text = memoryText,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                )
            }
        }

        // Animated Memory Icon with pulse effect
        Box(
            modifier = Modifier
                .size(56.dp)
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
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun StorageHeaderSection(
    storageInfo: StorageInfo,
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
                text = "Storage Status",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Storage Status Box
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                            )
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${usedPercentage}% Used • ${formatStorageSize(storageInfo.totalSpace)} Total",
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                )
            }
        }

        // Animated Storage Icon with pulse effect
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1976D2).copy(alpha = pulseAlpha * 0.6f),
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
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Storage,
                contentDescription = "Storage",
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun BatteryProgressSection(
    batteryInfo: BatteryInfo
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
                            text = "Charge Level",
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
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(batteryInfo.level / 100f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(5.dp))
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
        }
    }
}

@Composable
private fun MemoryProgressSection(
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // RAM Usage Progress Bar
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
                                text = "RAM Usage",
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

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(usedPercentage / 100f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(5.dp))
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

                // ZRAM Usage Progress Bar (only show if zram is available)
                if (memoryInfo.zramTotal > 0) {
                    val zramUsedPercentage = ((memoryInfo.zramUsed.toDouble() / memoryInfo.zramTotal.toDouble()) * 100).toInt()

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
                                    imageVector = Icons.Default.Compress,
                                    contentDescription = "Zram",
                                    tint = Color(0xFF9C27B0),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "ZRAM Usage",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "${zramUsedPercentage}%",
                                style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFF9C27B0)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(zramUsedPercentage / 100f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFF9C27B0),
                                                Color(0xFFBA68C8)
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }

                // Swap Usage Progress Bar (only show if swap is available)
                if (memoryInfo.swapTotal > 0) {
                    val swapUsedPercentage = ((memoryInfo.swapUsed.toDouble() / memoryInfo.swapTotal.toDouble()) * 100).toInt()

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
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Swap",
                                    tint = Color(0xFFFF5722),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Swap Usage",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "${swapUsedPercentage}%",
                                style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFFFF5722)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(swapUsedPercentage / 100f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFFFF5722),
                                                Color(0xFFFF7043)
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
}

@Composable
private fun BatteryStatsSection(
    batteryInfo: BatteryInfo
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
                        label = "Temperature",
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

                // Battery Stats Row 3 - Technology and Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Battery Technology
                    SystemStatItem(
                        icon = Icons.Default.Science,
                        label = "Technology",
                        value = batteryInfo.technology,
                        color = when (batteryInfo.technology.uppercase()) {
                            "LI-ION", "LITHIUM-ION" -> Color(0xFF2196F3)
                            "LI-PO", "LITHIUM-POLYMER" -> Color(0xFF9C27B0)
                            "NIMH" -> Color(0xFF4CAF50)
                            "NICD" -> Color(0xFF795548)
                            else -> MaterialTheme.colorScheme.outline
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // Battery Status
                    SystemStatItem(
                        icon = if (batteryInfo.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                        label = "Status",
                        value = batteryInfo.status,
                        color = when {
                            batteryInfo.status.contains("Charging", ignoreCase = true) -> Color(0xFF4CAF50)
                            batteryInfo.status.contains("Full", ignoreCase = true) -> Color(0xFF2196F3)
                            batteryInfo.status.contains("Discharging", ignoreCase = true) -> Color(0xFFFF9800)
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Battery Stats Row 4 - Current Capacity and Design Capacity (combined in one row)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
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

                    // Design Capacity (moved to same row to utilize space efficiently)
                    SystemStatItem(
                        icon = Icons.Default.BatterySaver,
                        label = "Design Cap",
                        value = if (batteryInfo.capacity > 0) "${batteryInfo.capacity}mAh" else "N/A",
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
private fun MemoryStatsSection(
    memoryInfo: MemoryInfo
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
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Memory Stats Row 1 - Used and Free RAM
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
                        value = "${memoryInfo.free / (1024 * 1024)}MB",
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Memory Stats Row 2 - Total RAM and Usage Percentage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Total RAM
                    SystemStatItem(
                        icon = Icons.Default.Widgets,
                        label = "Total RAM",
                        value = "${memoryInfo.total / (1024 * 1024)}MB",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )

                    // Usage Percentage
                    SystemStatItem(
                        icon = Icons.Default.Analytics,
                        label = "Usage %",
                        value = "${((memoryInfo.used.toDouble() / memoryInfo.total.toDouble()) * 100).toInt()}%",
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Memory Stats Row 3 - ZRAM Stats (only show if zram is available)
                if (memoryInfo.zramTotal > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // ZRAM Used
                        SystemStatItem(
                            icon = Icons.Default.Compress,
                            label = "ZRAM Used",
                            value = "${memoryInfo.zramUsed / (1024 * 1024)}MB",
                            color = Color(0xFF9C27B0),
                            modifier = Modifier.weight(1f)
                        )

                        // ZRAM Total
                        SystemStatItem(
                            icon = Icons.Default.Compress,
                            label = "ZRAM Total",
                            value = "${memoryInfo.zramTotal / (1024 * 1024)}MB",
                            color = Color(0xFFBA68C8),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Memory Stats Row 4 - Swap Stats (only show if swap is available)
                if (memoryInfo.swapTotal > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Swap Used
                        SystemStatItem(
                            icon = Icons.Default.SwapHoriz,
                            label = "Swap Used",
                            value = "${memoryInfo.swapUsed / (1024 * 1024)}MB",
                            color = Color(0xFFFF5722),
                            modifier = Modifier.weight(1f)
                        )

                        // Swap Total
                        SystemStatItem(
                            icon = Icons.Default.SwapHoriz,
                            label = "Swap Total",
                            value = "${memoryInfo.swapTotal / (1024 * 1024)}MB",
                            color = Color(0xFFFF7043),
                            modifier = Modifier.weight(1f)
                        )
                    }
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
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
    storageInfo: StorageInfo,
    modifier: Modifier = Modifier
) {
    SuperGlassCard(
        modifier = modifier,
        glassIntensity = GlassIntensity.Light,
        onClick = null
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Device Info Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Device Information",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Root Status Box
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (rooted) {
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFF4CAF50).copy(alpha = 0.8f),
                                                Color(0xFF66BB6A).copy(alpha = 0.6f)
                                            )
                                        )
                                    } else {
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                                            )
                                        )
                                    }
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (rooted) "Rooted" else "Not Rooted",
                                color = if (rooted) Color.White else MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                            )
                        }

                        // Version Box
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                                        )
                                    )
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "v$version",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }

                // Device Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Smartphone,
                        contentDescription = "Device",
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Device Stats (Storage Progress Section dihapus karena sudah ada Storage Card terpisah)
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
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Device Info Row 1
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            SystemStatItem(
                                icon = Icons.Default.PhoneAndroid,
                                label = "Model",
                                value = systemInfo.model,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )

                            SystemStatItem(
                                icon = Icons.Default.Code,
                                label = "Codename",
                                value = systemInfo.codename,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Device Info Row 2
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            SystemStatItem(
                                icon = Icons.Default.Android,
                                label = "Android",
                                value = systemInfo.androidVersion,
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.weight(1f)
                            )

                            SystemStatItem(
                                icon = Icons.Default.Build,
                                label = "SDK",
                                value = systemInfo.sdk.toString(),
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Device Info Row 3 - SoC and Fingerprint
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            SystemStatItem(
                                icon = Icons.Default.DeveloperBoard,
                                label = "SoC",
                                value = systemInfo.soc,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )

                            SystemStatItem(
                                icon = Icons.Default.Fingerprint,
                                label = "Fingerprint",
                                value = systemInfo.fingerprint.substringAfterLast("/").substringBefore(":"),
                                color = Color(0xFF2196F3),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Device Info Row 4 - Display Resolution and Technology
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            SystemStatItem(
                                icon = Icons.Default.AspectRatio,
                                label = "Resolution",
                                value = systemInfo.screenResolution,
                                color = Color(0xFFFF9800),
                                modifier = Modifier.weight(1f)
                            )

                            SystemStatItem(
                                icon = Icons.Default.DisplaySettings,
                                label = "Technology",
                                value = systemInfo.displayTechnology,
                                color = Color(0xFF9C27B0),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Device Info Row 5 - Refresh Rate and DPI
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            SystemStatItem(
                                icon = Icons.Default.Speed,
                                label = "Refresh Rate",
                                value = systemInfo.refreshRate,
                                color = Color(0xFF00BCD4),
                                modifier = Modifier.weight(1f)
                            )

                            SystemStatItem(
                                icon = Icons.Default.PhotoSizeSelectSmall,
                                label = "DPI",
                                value = systemInfo.screenDpi,
                                color = Color(0xFF795548),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Device Info Row 6 - GPU Renderer
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            SystemStatItem(
                                icon = Icons.Default.Videocam,
                                label = "GPU Renderer",
                                value = systemInfo.gpuRenderer,
                                color = Color(0xFFE91E63),
                                modifier = Modifier.weight(1f)
                            )

                            // Empty placeholder to maintain layout balance
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageProgressSection(
    storageInfo: StorageInfo
) {
    val usedPercentage = if (storageInfo.totalSpace > 0) {
        ((storageInfo.usedSpace.toDouble() / storageInfo.totalSpace.toDouble()) * 100).toInt()
    } else 0

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
                            imageVector = Icons.Default.Storage,
                            contentDescription = "Storage",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Internal Storage",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${formatStorageSize(storageInfo.usedSpace)} / ${formatStorageSize(storageInfo.totalSpace)}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                // Storage Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(usedPercentage / 100f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        when {
                                            usedPercentage < 70 -> Color(0xFF4CAF50)
                                            usedPercentage < 85 -> Color(0xFFFF9800)
                                            else -> Color(0xFFF44336)
                                        },
                                        when {
                                            usedPercentage < 70 -> Color(0xFF66BB6A)
                                            usedPercentage < 85 -> Color(0xFFFFB74D)
                                            else -> Color(0xFFEF5350)
                                        }
                                    )
                                )
                            )
                    )
                }

                // Storage Details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Used: ${formatStorageSize(storageInfo.usedSpace)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Free: ${formatStorageSize(storageInfo.totalSpace - storageInfo.usedSpace)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StorageStatsSection(
    storageInfo: StorageInfo
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
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Storage Stats Row 1 - Used and Free
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Used Storage
                    SystemStatItem(
                        icon = Icons.Default.Storage,
                        label = "Used Storage",
                        value = "${formatStorageSize(storageInfo.usedSpace)}",
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )

                    // Free Storage
                    SystemStatItem(
                        icon = Icons.Default.Storage,
                        label = "Free Storage",
                        value = "${formatStorageSize(storageInfo.totalSpace - storageInfo.usedSpace)}",
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Storage Stats Row 2 - Total and Usage Percentage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Total Storage
                    SystemStatItem(
                        icon = Icons.Default.Storage,
                        label = "Total Storage",
                        value = "${formatStorageSize(storageInfo.totalSpace)}",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )

                    // Usage Percentage
                    SystemStatItem(
                        icon = Icons.Default.Analytics,
                        label = "Usage %",
                        value = "${((storageInfo.usedSpace.toDouble() / storageInfo.totalSpace.toDouble()) * 100).toInt()}%",
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
