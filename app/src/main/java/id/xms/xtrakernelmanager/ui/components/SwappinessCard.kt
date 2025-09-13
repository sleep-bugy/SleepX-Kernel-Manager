package id.xms.xtrakernelmanager.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import id.xms.xtrakernelmanager.viewmodel.TuningViewModel
import kotlin.math.roundToInt

@Composable
fun SwappinessCard(
    vm: TuningViewModel,
    blur: Boolean
) {
    val zramEnabled by vm.zramEnabled.collectAsState()
    val zramDisksize by vm.zramDisksize.collectAsState()
    val maxZramSize by vm.maxZramSize.collectAsState()
    val swappiness by vm.swappiness.collectAsState()
    val compressionAlgorithms by vm.compressionAlgorithms.collectAsState()
    val currentCompression by vm.currentCompression.collectAsState()
    val dirtyRatio by vm.dirtyRatio.collectAsState()
    val dirtyBackgroundRatio by vm.dirtyBackgroundRatio.collectAsState()
    val dirtyWriteback by vm.dirtyWriteback.collectAsState()
    val dirtyExpireCentisecs by vm.dirtyExpireCentisecs.collectAsState()
    val minFreeMemory by vm.minFreeMemory.collectAsState()
    val swapSize by vm.swapSize.collectAsState()
    val maxSwapSize by vm.maxSwapSize.collectAsState()
    val isSwapLoading by vm.isSwapLoading.collectAsState()
    val swapLogs by vm.swapLogs.collectAsState()

    var isExpanded by remember { mutableStateOf(false) }

    // Dialog visibility states
    var showCompressionDialog by remember { mutableStateOf(false) }
    var showZramSizeDialog by remember { mutableStateOf(false) }
    var showSwappinessDialog by remember { mutableStateOf(false) }
    var showDirtyRatioDialog by remember { mutableStateOf(false) }
    var showDirtyBgRatioDialog by remember { mutableStateOf(false) }
    var showDirtyWritebackDialog by remember { mutableStateOf(false) }
    var showDirtyExpireDialog by remember { mutableStateOf(false) }
    var showMinFreeMemoryDialog by remember { mutableStateOf(false) }
    var showAdjustSwapSizeDialog by remember { mutableStateOf(false) }

    // Animation untuk pulse effect
    val infiniteTransition = rememberInfiniteTransition(label = "ram_control_pulse")
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
        modifier = Modifier,
        glassIntensity = GlassIntensity.Light,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // RAM Control Header Section
            RamControlHeaderSection(
                zramEnabled = zramEnabled,
                zramDisksize = zramDisksize,
                pulseAlpha = pulseAlpha,
                isExpanded = isExpanded,
                onExpandClick = { isExpanded = !isExpanded }
            )

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        thickness = 1.dp
                    )

                    // ZRAM Toggle Section
                    RamZramToggleSection(
                        zramEnabled = zramEnabled,
                        onZramToggle = { vm.setZramEnabled(!zramEnabled) }
                    )

                    AnimatedVisibility(visible = zramEnabled) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // ZRAM Settings
                            RamSettingItem(
                                icon = Icons.Default.Storage,
                                title = "ZRAM Size",
                                value = "${zramDisksize / (1024 * 1024)}MB",
                                description = "Adjust compressed RAM size",
                                color = MaterialTheme.colorScheme.secondary,
                                onClick = { showZramSizeDialog = true }
                            )

                            RamSettingItem(
                                icon = Icons.Default.Compress,
                                title = "Compression",
                                value = currentCompression,
                                description = "Compression algorithm",
                                color = MaterialTheme.colorScheme.tertiary,
                                onClick = { showCompressionDialog = true }
                            )
                        }
                    }

                    // Always visible RAM settings
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RamSettingItem(
                            icon = Icons.Default.Speed,
                            title = "Swappiness",
                            value = "$swappiness%",
                            description = "Memory swap aggressiveness",
                            color = MaterialTheme.colorScheme.primary,
                            onClick = { showSwappinessDialog = true }
                        )

                        RamSettingItem(
                            icon = Icons.Default.DataUsage,
                            title = "Dirty Ratio",
                            value = "$dirtyRatio%",
                            description = "Page cache dirty data threshold",
                            color = MaterialTheme.colorScheme.error,
                            onClick = { showDirtyRatioDialog = true }
                        )

                        RamSettingItem(
                            icon = Icons.Default.Analytics,
                            title = "Dirty Background Ratio",
                            value = "$dirtyBackgroundRatio%",
                            description = "Background writeback threshold",
                            color = MaterialTheme.colorScheme.surfaceTint,
                            onClick = { showDirtyBgRatioDialog = true }
                        )

                        RamSettingItem(
                            icon = Icons.Default.Timer,
                            title = "Dirty Writeback",
                            value = "${dirtyWriteback}s",
                            description = "Writeback interval time",
                            color = MaterialTheme.colorScheme.outline,
                            onClick = { showDirtyWritebackDialog = true }
                        )

                        RamSettingItem(
                            icon = Icons.Default.Schedule,
                            title = "Dirty Expire",
                            value = "${dirtyExpireCentisecs}cs",
                            description = "Page expiration time",
                            color = MaterialTheme.colorScheme.secondary,
                            onClick = { showDirtyExpireDialog = true }
                        )

                        RamSettingItem(
                            icon = Icons.Default.Memory,
                            title = "Min Free Memory",
                            value = "${minFreeMemory}MB",
                            description = "Minimum free memory reserve",
                            color = MaterialTheme.colorScheme.tertiary,
                            onClick = { showMinFreeMemoryDialog = true }
                        )

                        RamSettingItem(
                            icon = Icons.Default.SwapHoriz,
                            title = "Swap Size",
                            value = if (swapSize == 0L) "Disabled" else "${swapSize / (1024 * 1024)}MB",
                            description = "Virtual memory swap file size",
                            color = MaterialTheme.colorScheme.primary,
                            onClick = { showAdjustSwapSizeDialog = true }
                        )
                    }
                }
            }
        }
    }

    // --- DIALOGS ---
    if (showZramSizeDialog && zramEnabled) {
        ZramSizeDialog(
            currentSize = zramDisksize,
            maxSize = maxZramSize,
            onDismiss = { showZramSizeDialog = false },
            onConfirm = { newSizeInBytes: Long ->
                vm.setZramDisksize(newSizeInBytes)
                showZramSizeDialog = false
            }
        )
    }

    if (showCompressionDialog && zramEnabled) {
        CompressionAlgorithmDialog(
            compressionAlgorithms = compressionAlgorithms,
            currentCompression = currentCompression,
            onDismiss = { showCompressionDialog = false },
            onAlgorithmSelected = { algo: String ->
                if (algo != currentCompression) {
                    vm.setCompression(algo)
                }
                showCompressionDialog = false
            }
        )
    }

    if (showSwappinessDialog) {
        SliderSettingDialog(
            showDialog = showSwappinessDialog,
            title = "Set Swappiness",
            currentValue = swappiness,
            valueSuffix = "%",
            valueRange = 0f..100f,
            steps = 99,
            onDismissRequest = { showSwappinessDialog = false },
            onApplyClicked = { newValue: Int ->
                vm.setSwappiness(newValue)
                showSwappinessDialog = false
            }
        )
    }

    if (showDirtyRatioDialog) {
        SliderSettingDialog(
            showDialog = showDirtyRatioDialog,
            title = "Set Dirty Ratio",
            currentValue = dirtyRatio,
            valueSuffix = "%",
            valueRange = 0f..100f,
            steps = 99,
            onDismissRequest = { showDirtyRatioDialog = false },
            onApplyClicked = { newValue: Int ->
                vm.setDirtyRatio(newValue)
                showDirtyRatioDialog = false
            }
        )
    }

    if (showDirtyBgRatioDialog) {
        SliderSettingDialog(
            showDialog = showDirtyBgRatioDialog,
            title = "Set Dirty Background Ratio",
            currentValue = dirtyBackgroundRatio,
            valueSuffix = "%",
            valueRange = 0f..100f,
            steps = 99,
            onDismissRequest = { showDirtyBgRatioDialog = false },
            onApplyClicked = { newValue: Int ->
                vm.setDirtyBackgroundRatio(newValue)
                showDirtyBgRatioDialog = false
            }
        )
    }

    if (showDirtyWritebackDialog) {
        SliderSettingDialog(
            showDialog = showDirtyWritebackDialog,
            title = "Set Dirty Writeback",
            currentValue = dirtyWriteback,
            valueSuffix = " sec",
            valueRange = 0f..300f,
            steps = 299,
            onDismissRequest = { showDirtyWritebackDialog = false },
            onApplyClicked = { newValue: Int ->
                vm.setDirtyWriteback(newValue)
                showDirtyWritebackDialog = false
            }
        )
    }

    if (showDirtyExpireDialog) {
        SliderSettingDialog(
            showDialog = showDirtyExpireDialog,
            title = "Set Dirty Expire",
            currentValue = dirtyExpireCentisecs,
            valueSuffix = " cs",
            valueRange = 0f..30000f,
            steps = 29999,
            onDismissRequest = { showDirtyExpireDialog = false },
            onApplyClicked = { newValue: Int ->
                vm.setDirtyExpireCentisecs(newValue)
                showDirtyExpireDialog = false
            }
        )
    }

    if (showMinFreeMemoryDialog) {
        SliderSettingDialog(
            showDialog = showMinFreeMemoryDialog,
            title = "Set Min Free Memory",
            currentValue = minFreeMemory,
            valueSuffix = " MB",
            valueRange = 0f..1024f,
            steps = 1023,
            onDismissRequest = { showMinFreeMemoryDialog = false },
            onApplyClicked = { newValue: Int ->
                vm.setMinFreeMemory(newValue)
                showMinFreeMemoryDialog = false
            }
        )
    }

    if (showAdjustSwapSizeDialog) {
        SwapSizeDialog(
            currentSize = swapSize,
            maxSize = maxSwapSize,
            onDismiss = { showAdjustSwapSizeDialog = false },
            onConfirm = { newSizeInBytes: Long ->
                vm.setSwapSize(newSizeInBytes)
                showAdjustSwapSizeDialog = false
            }
        )
    }

    if (isSwapLoading) {
        SwapLoadingDialog(logs = swapLogs)
    }
}

// --- BAGIAN UTAMA YANG DIPERBARUI ---

// Ganti fungsi SliderSettingDialog yang lama dengan ini
@Composable
fun SliderSettingDialog(
    showDialog: Boolean,
    title: String,
    currentValue: Int,
    valueSuffix: String = "",
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onDismissRequest: () -> Unit,
    onApplyClicked: (Int) -> Unit
) {
    if (!showDialog) return

    var sliderTempValue by remember(currentValue) { mutableStateOf(currentValue.toFloat()) }
    val icon = when {
        title.contains("Swappiness", ignoreCase = true) -> Icons.Default.SwapVert
        title.contains("Dirty Ratio", ignoreCase = true) -> Icons.Default.DataUsage
        title.contains("Dirty Background", ignoreCase = true) -> Icons.Default.Analytics
        title.contains("Dirty Writeback", ignoreCase = true) -> Icons.Default.Timer
        title.contains("Dirty Expire", ignoreCase = true) -> Icons.Default.Schedule
        title.contains("Min Free Memory", ignoreCase = true) -> Icons.Default.Memory
        else -> Icons.Default.Tune
    }
    val color = when {
        title.contains("Swappiness", ignoreCase = true) -> MaterialTheme.colorScheme.primary
        title.contains("Dirty Ratio", ignoreCase = true) -> MaterialTheme.colorScheme.error
        title.contains("Dirty Background", ignoreCase = true) -> MaterialTheme.colorScheme.surfaceTint
        title.contains("Dirty Writeback", ignoreCase = true) -> MaterialTheme.colorScheme.outline
        title.contains("Dirty Expire", ignoreCase = true) -> MaterialTheme.colorScheme.secondary
        title.contains("Min Free Memory", ignoreCase = true) -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    val explanation = when {
        title.contains("Swappiness", ignoreCase = true) ->
            "Swappiness controls how aggressively the kernel swaps memory pages. Lower values keep more data in RAM for performance, higher values swap more to free up RAM."
        title.contains("Dirty Ratio", ignoreCase = true) ->
            "Maximum percent of RAM that can be filled with dirty pages before writeback."
        title.contains("Dirty Background", ignoreCase = true) ->
            "Percent of RAM at which background writeback starts."
        title.contains("Dirty Writeback", ignoreCase = true) ->
            "Interval (in seconds) for periodic writeback of dirty pages."
        title.contains("Dirty Expire", ignoreCase = true) ->
            "Time (in centiseconds) after which dirty data is considered old and must be written."
        title.contains("Min Free Memory", ignoreCase = true) ->
            "Minimum amount of RAM (in MB) the kernel tries to keep free."
        else -> "Adjust this kernel parameter to fine-tune system performance and memory management."
    }
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        color.copy(alpha = 0.4f),
                                        color.copy(alpha = 0.2f),
                                        Color.Transparent
                                    ),
                                    radius = 60f
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            modifier = Modifier.size(24.dp),
                            tint = color
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = valueSuffix.trim().ifEmpty { "Kernel Parameter" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                // Explanation
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            modifier = Modifier.size(18.dp),
                            tint = color
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = explanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            lineHeight = 18.sp
                        )
                    }
                }
                // Slider
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${sliderTempValue.roundToInt()}$valueSuffix",
                        style = MaterialTheme.typography.headlineMedium,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = sliderTempValue,
                        onValueChange = { sliderTempValue = it },
                        valueRange = valueRange,
                        steps = if (steps > 0) steps - 1 else 0,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = color,
                            activeTrackColor = color,
                            inactiveTrackColor = color.copy(alpha = 0.3f)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${valueRange.start.roundToInt()}$valueSuffix",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${valueRange.endInclusive.roundToInt()}$valueSuffix",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onApplyClicked(sliderTempValue.roundToInt())
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Apply", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


@Composable
fun RamControlHeaderSection(
    zramEnabled: Boolean,
    zramDisksize: Long,
    pulseAlpha: Float,
    isExpanded: Boolean,
    onExpandClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpandClick() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "RAM Control",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            // RAM Status Box
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
                    text = if (zramEnabled) "ZRAM: ${zramDisksize / (1024 * 1024)}MB Active" else "ZRAM: Disabled",
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                )
            }
        }

        // Animated RAM Icon with pulse effect
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF9C27B0).copy(alpha = pulseAlpha * 0.6f),
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
                    imageVector = Icons.Default.Memory,
                    contentDescription = "RAM Control",
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
            }

            Icon(
                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun RamZramToggleSection(
    zramEnabled: Boolean,
    onZramToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (zramEnabled) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                }
            )
            .clickable { onZramToggle() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (zramEnabled) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (zramEnabled) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                    contentDescription = "ZRAM Toggle",
                    modifier = Modifier.size(24.dp),
                    tint = if (zramEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Column {
                Text(
                    text = "ZRAM State",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = if (zramEnabled) "Compressed RAM enabled" else "Compressed RAM disabled",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        Text(
            text = if (zramEnabled) "ON" else "OFF",
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = if (zramEnabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            }
        )
    }
}

@Composable
fun RamSettingItem(
    icon: ImageVector,
    title: String,
    value: String,
    description: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(20.dp),
                tint = color
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = color
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Configure",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

// Helper function to get feature explanations
private fun getFeatureExplanation(title: String): String {
    return when {
        title.contains("Swappiness", ignoreCase = true) ->
            "Controls kernel's tendency to swap. Lower values (e.g., 10) keep data in RAM longer for better performance. Higher values (e.g., 100) swap more aggressively to free up RAM."

        title.contains("ZRAM", ignoreCase = true) ->
            "Sets the size of the compressed RAM block. Acts as a fast swap space, improving multitasking on devices with less RAM."

        title.contains("Dirty Ratio", ignoreCase = true) ->
            "Max percentage of total memory that can hold dirty pages before processes are forced to write them to disk."

        title.contains("Dirty Background", ignoreCase = true) ->
            "Percentage of total memory where background processes start writing dirty pages to disk."

        title.contains("Min Free Memory", ignoreCase = true) ->
            "Tells the kernel to keep a certain amount of RAM free. A larger value can improve responsiveness but reduces memory available for apps."

        title.contains("Dirty Expire", ignoreCase = true) ->
            "How long a dirty page can stay in cache before it must be written to disk (in centiseconds)."

        title.contains("Dirty Writeback", ignoreCase = true) ->
            "How often the kernel's writeback daemons wake up to write dirty data to disk (in seconds)."

        else ->
            "Adjust this kernel parameter to fine-tune system performance and memory management."
    }
}

// Helper function to get appropriate icons
private fun getFeatureIcon(title: String): ImageVector {
    return when {
        title.contains("Swappiness", ignoreCase = true) -> Icons.Default.SwapVert
        title.contains("ZRAM", ignoreCase = true) -> Icons.Default.Compress
        title.contains("Dirty", ignoreCase = true) -> Icons.Default.Storage
        title.contains("Memory", ignoreCase = true) -> Icons.Default.Memory
        else -> Icons.Default.Tune
    }
}

// Placeholder dialog composables - these need to be implemented based on your existing dialogs
@Composable
fun ZramSizeDialog(
    currentSize: Long,
    maxSize: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var sliderTempValue by remember(currentSize) { mutableStateOf((currentSize / (1024 * 1024)).toFloat()) }
    val minValue = 128f
    val maxValue = (maxSize / (1024 * 1024)).toFloat()
    val step = 128f
    val steps = ((maxValue - minValue) / step).toInt()
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                                        Color.Transparent
                                    ),
                                    radius = 60f
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = "ZRAM Size",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Set ZRAM Size",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Compressed RAM Block Size",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                // Explanation
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ZRAM provides a compressed block device in RAM, acting as a fast swap space. Increasing ZRAM size can improve multitasking but reduces available RAM for apps.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            lineHeight = 18.sp
                        )
                    }
                }
                // Slider
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${sliderTempValue.roundToInt()} MB",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = sliderTempValue,
                        onValueChange = { sliderTempValue = (it / step).roundToInt() * step },
                        valueRange = minValue..maxValue,
                        steps = steps - 1,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.secondary,
                            activeTrackColor = MaterialTheme.colorScheme.secondary,
                            inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${minValue.roundToInt()} MB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${maxValue.roundToInt()} MB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onConfirm(sliderTempValue.roundToInt() * 1024 * 1024L)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Apply", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
// Ganti fungsi CompressionAlgorithmDialog yang lama dengan ini
@Composable
fun CompressionAlgorithmDialog(
    compressionAlgorithms: List<String>,
    currentCompression: String,
    onDismiss: () -> Unit,
    onAlgorithmSelected: (String) -> Unit
) {
    var selectedAlgorithm by remember { mutableStateOf(currentCompression) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                        Color.Transparent
                                    ),
                                    radius = 60f
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Compress,
                            contentDescription = "Compression",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Choose Compression Algorithm",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "ZRAM Performance Settings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                // Explanation
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "The compression algorithm used by ZRAM to compress data in memory. LZ4 prioritizes speed, ZSTD high efficiency, LZO optimal balance between speed and compression ratio.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            lineHeight = 18.sp
                        )
                    }
                }
                // Algorithm selection list
                LazyColumn(
                    modifier = Modifier.heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(compressionAlgorithms) { algorithm ->
                        val isSelected = algorithm == selectedAlgorithm
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAlgorithm = algorithm }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedAlgorithm = algorithm },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary,
                                        unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = algorithm.uppercase(),
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = when (algorithm.lowercase()) {
                                            "lz4" -> "Maximum speed • Low latency"
                                            "zstd" -> "High efficiency • Best compression ratio"
                                            "lzo" -> "Optimal balance • Stable"
                                            "lz4hc" -> "High compression • Moderate speed"
                                            "deflate" -> "Standard compression • Moderate speed"
                                            "lzma" -> "Maximum compression • Slower"
                                            "bzip2" -> "Better compression • Slower"
                                            "zlib" -> "Balanced compression • Moderate speed"
                                            "lzo-rle" -> "Fast compression • Low CPU usage"
                                            else -> "Compression algorithm • Good performance"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onAlgorithmSelected(selectedAlgorithm)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Apply", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SwapSizeDialog(
    currentSize: Long,
    maxSize: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    SliderSettingDialog(
        showDialog = true,
        title = "Set Swap Size",
        currentValue = (currentSize / (1024 * 1024)).toInt(),
        valueSuffix = " MB",
        valueRange = 0f..(maxSize / (1024 * 1024)).toFloat(),
        steps = (maxSize / (1024 * 1024)).toInt() / 128,
        onDismissRequest = onDismiss,
        onApplyClicked = { newValue ->
            onConfirm(newValue * 1024L * 1024L)
        }
    )
}

// Ganti fungsi SwapLoadingDialog yang lama dengan ini
@Composable
fun SwapLoadingDialog(
    logs: List<String>
) {
    val listState = rememberLazyListState()
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Dialog(
        onDismissRequest = { /* Cannot be dismissed */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 12.dp,
            shadowElevation = 16.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    "Configuring Swap...",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                // Expressive Material 3 loading indicator
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                    Color.Transparent
                                ),
                                radius = 48f
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(44.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                        strokeWidth = 5.dp
                    )
                }

                if (logs.isNotEmpty()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 150.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(8.dp)
                    ) {
                        items(logs) { log ->
                            Text(
                                log,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}