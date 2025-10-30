package id.xms.xtrakernelmanager.ui.components

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import id.xms.xtrakernelmanager.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.xms.xtrakernelmanager.data.model.BatteryInfo
import id.xms.xtrakernelmanager.data.model.DeepSleepInfo
import id.xms.xtrakernelmanager.data.model.MemoryInfo
import id.xms.xtrakernelmanager.data.model.StorageInfo
import id.xms.xtrakernelmanager.data.model.SystemInfo
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

/* ---------- helper ---------- */
private fun formatTimeWithSeconds(timeInMillis: Long): String {
    val totalSeconds = timeInMillis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
}

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

/* ---------- main ---------- */
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
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BatteryCard(batteryInfo = b, blur = blur)
        MemoryCard(memoryInfo = mem, blur = blur)
        StorageCard(storageInfo = storageInfo, blur = blur)
        DeviceInfoCard(
            systemInfo = systemInfo,
            rooted = rooted,
            version = version,
            blur = blur,
            storageInfo = storageInfo
        )
    }
}

/* ---------- cards ---------- */
@Composable
private fun BatteryCard(
    batteryInfo: BatteryInfo,
    blur: Boolean,
    modifier: Modifier = Modifier
) {
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
            BatteryHeaderSectionMinimal(batteryInfo = batteryInfo)
            BatteryProgressSection(batteryInfo = batteryInfo)
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
            MemoryHeaderSectionMinimal(memoryInfo = memoryInfo, usedPercentage = usedPercentage)
            MemoryProgressSection(memoryInfo = memoryInfo, usedPercentage = usedPercentage)
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
            StorageHeaderSectionMinimal(storageInfo = storageInfo, usedPercentage = usedPercentage)
            StorageProgressSection(storageInfo = storageInfo)
            StorageStatsSection(storageInfo = storageInfo)
        }
    }
}

/* ---------- minimal headers (lightweight) ---------- */
@Composable
private fun BatteryHeaderSectionMinimal(
    batteryInfo: BatteryInfo,
    modifier: Modifier = Modifier
) {
    val statusText = when {
        batteryInfo.status.contains("Full", true) -> "Full"
        batteryInfo.status.contains("Charging", true) -> "Charging"
        else -> "Not Charging"
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Battery", style = MaterialTheme.typography.titleLarge)
            AssistChip(onClick = {}, label = { Text(text = "${batteryInfo.level}% • $statusText") })
        }
        Icon(
            imageVector = Icons.Default.BatteryChargingFull,
            contentDescription = "Battery",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun MemoryHeaderSectionMinimal(
    memoryInfo: MemoryInfo,
    usedPercentage: Int,
    modifier: Modifier = Modifier
) {
    val totalGb = (memoryInfo.total / (1024 * 1024 * 1024))
    val zramGb = (memoryInfo.zramTotal / (1024 * 1024 * 1024))
    val swapGb = (memoryInfo.swapTotal / (1024 * 1024 * 1024))
    val memoryText = buildString {
        append("${usedPercentage}% Used · ${totalGb}GB")
        if (zramGb > 0) append(" + ${zramGb}GB Zram")
        if (swapGb > 0) append(" + ${swapGb}GB Swap")
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Memory", style = MaterialTheme.typography.titleLarge)
            AssistChip(onClick = {}, label = { Text(text = memoryText) })
        }
        Icon(
            imageVector = Icons.Default.Memory,
            contentDescription = "Memory",
            tint = MaterialTheme.colorScheme.tertiary
        )
    }
}

@Composable
private fun StorageHeaderSectionMinimal(
    storageInfo: StorageInfo,
    usedPercentage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Storage", style = MaterialTheme.typography.titleLarge)
            AssistChip(
                onClick = {},
                label = { Text(text = "${usedPercentage}% Used · ${formatStorageSize(storageInfo.totalSpace)} Total") }
            )
        }
        Icon(
            imageVector = Icons.Default.Storage,
            contentDescription = "Storage",
            tint = MaterialTheme.colorScheme.secondary
        )
    }
}

/* ---------- battery header ---------- */
@Composable
private fun BatteryHeaderSection(
    batteryInfo: BatteryInfo,
    pulseAlpha: Float
) {
    /* ====== PERBAIKAN UTAMA ====== */
    val hardCharging = rememberHardCharging()
    val statusText = when {
        hardCharging -> "Charging"
        batteryInfo.status.contains("Full", ignoreCase = true) -> "Full"
        else -> "Not Charging"
    }
    val icon = when {
        hardCharging                                -> Icons.Default.BatteryChargingFull
        batteryInfo.status.contains("Full", true)   -> Icons.Default.BatteryFull
        else                                        -> Icons.Default.BatteryStd
    }


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
                    text = "${batteryInfo.level}% • $statusText",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                )
            }
        }

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
                imageVector = icon,
                contentDescription = "Battery",
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/* ---------- memory header ---------- */
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

/* ---------- storage header ---------- */
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

/* ---------- progress sections ---------- */
@Composable
private fun BatteryProgressSection(batteryInfo: BatteryInfo) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BatteryFull, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(id = R.string.label_charge_level), style = MaterialTheme.typography.titleSmall)
            }
            Text("${batteryInfo.level}%", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        }
        LinearProgressIndicator(
            progress = { (batteryInfo.level / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = when {
                batteryInfo.level > 70 -> Color(0xFF6C9BFF)
                batteryInfo.level > 30 -> Color(0xFFE6B566)
                else -> MaterialTheme.colorScheme.error
            }
        )
    }
}

@Composable
private fun MemoryProgressSection(memoryInfo: MemoryInfo, usedPercentage: Int) {
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
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${usedPercentage}%",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
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
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "${zramUsedPercentage}%",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
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
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "${swapUsedPercentage}%",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
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


/* ---------- stats sections ---------- */
@Composable
private fun BatteryStatsSection(batteryInfo: BatteryInfo) {
    val hardCharging = rememberHardCharging()
    val statusText = when {
        hardCharging                                -> "Charging"
        batteryInfo.status.contains("Full", true)   -> "Full"
        else                                        -> "Not Charging"
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
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

                    SystemStatItem(
                        icon = when {
                            batteryInfo.health.contains("Overvoltage", ignoreCase = true) -> Icons.Default.BatteryAlert
                            hardCharging -> Icons.Default.BatteryChargingFull
                            batteryInfo.status.contains("Full", ignoreCase = true) -> Icons.Default.BatteryFull
                            else -> Icons.Default.BatteryStd
                        },
                        label = "Status",
                        value = statusText,
                        color = when {
                            batteryInfo.health.contains("Overvoltage", ignoreCase = true) -> MaterialTheme.colorScheme.error
                            hardCharging -> Color(0xFF4CAF50)
                            batteryInfo.status.contains("Full", ignoreCase = true) -> Color(0xFF2196F3)
                            batteryInfo.status.contains("Discharging", ignoreCase = true) -> Color(0xFFFF9800)
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
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

                    SystemStatItem(
                        icon = Icons.Default.BatterySaver,
                        label = "Design Cap",
                        value = if (batteryInfo.capacity > 0) "${batteryInfo.capacity}mAh" else "N/A",
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (batteryInfo.current != 0f) {
                    val currentInMa = batteryInfo.current / 1000f
                    val isDeviceConsideredCharging = hardCharging

                    val correctedSignedMa = if (currentInMa == 0f) {
                        0f
                    } else if (isDeviceConsideredCharging) {
                        kotlin.math.abs(currentInMa)
                    } else {
                        -kotlin.math.abs(currentInMa)
                    }

                    val displayCurrentText = String.format(Locale.getDefault(), "%.0f", correctedSignedMa)
                    val iconForCurrent = if (isDeviceConsideredCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryAlert
                    val colorForCurrent = when {
                        correctedSignedMa > 0f -> Color(0xFF4CAF50)
                        correctedSignedMa < 0f -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    SystemStatItem(
                        icon = iconForCurrent,
                        label = "Current",
                        value = "${displayCurrentText}mA",
                        color = colorForCurrent,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
    }
}

@Composable
private fun MemoryStatsSection(memoryInfo: MemoryInfo) {
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SystemStatItem(
                        icon = Icons.Default.Memory,
                        label = "Used RAM",
                        value = "${memoryInfo.used / (1024 * 1024)}MB",
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )

                    SystemStatItem(
                        icon = Icons.Default.Storage,
                        label = "Free RAM",
                        value = "${memoryInfo.free / (1024 * 1024)}MB",
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SystemStatItem(
                        icon = Icons.Default.Widgets,
                        label = "Total RAM",
                        value = "${memoryInfo.total / (1024 * 1024)}MB",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )

                    SystemStatItem(
                        icon = Icons.Default.Analytics,
                        label = "Usage %",
                        value = "${((memoryInfo.used.toDouble() / memoryInfo.total.toDouble()) * 100).toInt()}%",
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (memoryInfo.zramTotal > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SystemStatItem(
                            icon = Icons.Default.Compress,
                            label = "ZRAM Used",
                            value = "${memoryInfo.zramUsed / (1024 * 1024)}MB",
                            color = Color(0xFF9C27B0),
                            modifier = Modifier.weight(1f)
                        )

                        SystemStatItem(
                            icon = Icons.Default.Compress,
                            label = "ZRAM Total",
                            value = "${memoryInfo.zramTotal / (1024 * 1024)}MB",
                            color = Color(0xFFBA68C8),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (memoryInfo.swapTotal > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SystemStatItem(
                            icon = Icons.Default.SwapHoriz,
                            label = "Swap Used",
                            value = "${memoryInfo.swapUsed / (1024 * 1024)}MB",
                            color = Color(0xFFFF5722),
                            modifier = Modifier.weight(1f)
                        )

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
private fun StorageProgressSection(storageInfo: StorageInfo) {
    val usedPercentage = if (storageInfo.totalSpace > 0) {
        ((storageInfo.usedSpace.toDouble() / storageInfo.totalSpace.toDouble()) * 100).toInt()
    } else 0

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(id = R.string.label_internal_storage), style = MaterialTheme.typography.titleSmall)
            }
            Text(
                text = "${formatStorageSize(storageInfo.usedSpace)} / ${formatStorageSize(storageInfo.totalSpace)}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        LinearProgressIndicator(
            progress = { (usedPercentage / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = when {
                usedPercentage < 70 -> Color(0xFF6C9BFF)
                usedPercentage < 85 -> Color(0xFFE6B566)
                else -> MaterialTheme.colorScheme.error
            }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(id = R.string.label_used) + ": ${formatStorageSize(storageInfo.usedSpace)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(id = R.string.label_free) + ": ${formatStorageSize(storageInfo.totalSpace - storageInfo.usedSpace)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StorageStatsSection(storageInfo: StorageInfo) {
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SystemStatItem(
                        icon = Icons.Default.Storage,
                        label = "Used Storage",
                        value = "${formatStorageSize(storageInfo.usedSpace)}",
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )

                    SystemStatItem(
                        icon = Icons.Default.Storage,
                        label = "Free Storage",
                        value = "${formatStorageSize(storageInfo.totalSpace - storageInfo.usedSpace)}",
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SystemStatItem(
                        icon = Icons.Default.Storage,
                        label = "Total Storage",
                        value = "${formatStorageSize(storageInfo.totalSpace)}",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )

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

/* ---------- device info ---------- */
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

                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

/* ---------- small composables ---------- */
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
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

//private fun isRootGranted(): Boolean =
//    try {
//        Runtime.getRuntime().exec("su -c id").waitFor() == 0
//    } catch (e: Exception) { false }
//
//private object PowerSupply {
//    fun readInt(path: String): Int =
//        File(path).bufferedReader().use { it.readText().trim().toIntOrNull() ?: -1 }
//
//    fun readString(path: String): String =
//        File(path).bufferedReader().use { it.readText().trim() }
//
//    /* true = cable masih nyambung, false = sudah dicabut */
//    fun isChargingSysfs(): Boolean =
//        when {
//            readInt("/sys/class/power_supply/battery/online") == 1 -> true
//            readString("/sys/class/power_supply/battery/status").equals("charging", true) -> true
//            else -> false
//        }
//}

/* -------------------------------------------------
   DETEKSI CHARGING DARI API ANDROID (NO ROOT)
   ------------------------------------------------- */
@Composable
private fun rememberHardCharging(): Boolean {
    val context = LocalContext.current
    /* sticky intent tidak perlu register/unregister */
    val intent: Intent? = remember {
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }
    val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    return status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
}
