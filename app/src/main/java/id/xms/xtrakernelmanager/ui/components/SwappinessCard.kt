package id.xms.xtrakernelmanager.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke // Keep for Compression Dialog
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.xms.xtrakernelmanager.viewmodel.TuningViewModel
import kotlin.math.roundToInt

@Composable
fun SwappinessCard(
    vm: TuningViewModel,
    blur: Boolean
) {
    val zramEnabled by vm.zramEnabled.collectAsState()
    val zramDisksize by vm.zramDisksize.collectAsState() // Bytes
    val maxZramSize by vm.maxZramSize.collectAsState() // Bytes
    val swappiness by vm.swappiness.collectAsState()
    val compressionAlgorithms by vm.compressionAlgorithms.collectAsState()
    val currentCompression by vm.currentCompression.collectAsState()
    val dirtyRatio by vm.dirtyRatio.collectAsState()
    val dirtyBackgroundRatio by vm.dirtyBackgroundRatio.collectAsState()
    val dirtyWriteback by vm.dirtyWriteback.collectAsState() // Seconds
    val dirtyExpireCentisecs by vm.dirtyExpireCentisecs.collectAsState() // Centiseconds
    val minFreeMemory by vm.minFreeMemory.collectAsState() // MB
    val swapSize by vm.swapSize.collectAsState() // Bytes
    val maxSwapSize by vm.maxSwapSize.collectAsState() // Bytes
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


    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "DropdownRotation"
    )

    GlassCard(blur) {
        Column(
            Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "RAM Control",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = if (isExpanded) "Collapse RAM Control" else "Expand RAM Control",
                    modifier = Modifier.rotate(rotationAngle),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = slideInVertically(
                    initialOffsetY = { -it / 2 },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                ) + expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                ) + fadeIn(
                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { -it / 2 },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                ) + shrinkVertically(
                    shrinkTowards = Alignment.Top,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                ) + fadeOut(
                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                )
            ) {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // ZRAM State
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(MaterialTheme.shapes.small)
                            .clickable { vm.setZramEnabled(!zramEnabled) }
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ZRAM State",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (zramEnabled) {
                                Text(
                                    text = " (${zramDisksize / (1024 * 1024)}MB)",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (zramEnabled) "ON" else "OFF",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (zramEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                ),
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Icon(
                                imageVector = if (zramEnabled) Icons.Filled.ToggleOn else Icons.Filled.ToggleOff,
                                contentDescription = if (zramEnabled) "Turn ZRAM Off" else "Turn ZRAM On",
                                modifier = Modifier
                                    .size(48.dp)
                                    .clickable(onClick = { vm.setZramEnabled(!zramEnabled) }),
                                tint = if (zramEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }

                    AnimatedVisibility(visible = zramEnabled) {
                        Column {
                            // Adjust ZRAM Size Row
                            ClickableSettingRow(
                                title = "Adjust ZRAM Size",
                                value = "${zramDisksize / (1024 * 1024)}MB",
                                onClick = { showZramSizeDialog = true }
                            )

                            // Compression Algorithm Row
                            ClickableSettingRow(
                                title = "Compression",
                                value = currentCompression,
                                onClick = { showCompressionDialog = true }
                            )

                            // Swappiness Row
                            ClickableSettingRow(
                                title = "Swappiness",
                                value = "$swappiness%",
                                onClick = { showSwappinessDialog = true }
                            )
                        }
                    }

                    // Adjust Swap Size Row
                    ClickableSettingRow(
                        title = "Adjust Swap Size",
                        value = if (swapSize > 0) "${swapSize / (1024 * 1024)}MB" else "Disabled",
                        onClick = { showAdjustSwapSizeDialog = true }
                    )



                    // Memory Management Section
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Memory Management",
                        style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.tertiary),
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )

                    // Dirty Ratio Row
                    ClickableSettingRow(
                        title = "Dirty Ratio",
                        value = "$dirtyRatio%",
                        onClick = { showDirtyRatioDialog = true }
                    )

                    // Dirty Background Ratio Row
                    ClickableSettingRow(
                        title = "Dirty Background Ratio",
                        value = "$dirtyBackgroundRatio%",
                        onClick = { showDirtyBgRatioDialog = true }
                    )

                    // Dirty Writeback Row
                    ClickableSettingRow(
                        title = "Dirty Writeback",
                        value = "$dirtyWriteback sec",
                        onClick = { showDirtyWritebackDialog = true }
                    )

                    // Dirty Expire Row
                    ClickableSettingRow(
                        title = "Dirty Expire",
                        value = "$dirtyExpireCentisecs cs",
                        onClick = { showDirtyExpireDialog = true }
                    )

                    // Minimum Free Memory Row
                    ClickableSettingRow(
                        title = "Min Free Memory",
                        value = "$minFreeMemory MB",
                        onClick = { showMinFreeMemoryDialog = true }
                    )
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
            onConfirm = { newSizeInBytes ->
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
            onAlgorithmSelected = { algo ->
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
            onApplyClicked = { newValue ->
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
            onApplyClicked = { newValue ->
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
            onApplyClicked = { newValue ->
                vm.setDirtyBackgroundRatio(newValue)
                showDirtyBgRatioDialog = false
            }
        )
    }

    if (showDirtyWritebackDialog) {
        SliderSettingDialog(
            showDialog = showDirtyWritebackDialog,
            title = "Set Dirty Writeback",
            currentValue = dirtyWriteback, // Assuming vm.dirtyWriteback is Int in seconds
            valueSuffix = " sec",
            valueRange = 0f..300f, // 0 to 300 seconds
            steps = 299,
            onDismissRequest = { showDirtyWritebackDialog = false },
            onApplyClicked = { newValue ->
                vm.setDirtyWriteback(newValue) // vm.setDirtyWriteback expects Int in seconds
                showDirtyWritebackDialog = false
            }
        )
    }

    if (showDirtyExpireDialog) {
        SliderSettingDialog(
            showDialog = showDirtyExpireDialog,
            title = "Set Dirty Expire",
            currentValue = dirtyExpireCentisecs, // Assuming vm.dirtyExpireCentisecs is Int in centiseconds
            valueSuffix = " cs",
            valueRange = 0f..30000f, // 0 to 30000 centiseconds
            steps = 29999,
            onDismissRequest = { showDirtyExpireDialog = false },
            onApplyClicked = { newValue ->
                vm.setDirtyExpireCentisecs(newValue) // vm.setDirtyExpireCentisecs expects Int in centiseconds
                showDirtyExpireDialog = false
            }
        )
    }

    if (showMinFreeMemoryDialog) {
        SliderSettingDialog(
            showDialog = showMinFreeMemoryDialog,
            title = "Set Min Free Memory",
            currentValue = minFreeMemory, // Assuming vm.minFreeMemory is Int in MB
            valueSuffix = " MB",
            valueRange = 0f..1024f, // 0 to 1024 MB
            steps = 1023,
            onDismissRequest = { showMinFreeMemoryDialog = false },
            onApplyClicked = { newValue ->
                vm.setMinFreeMemory(newValue) // vm.setMinFreeMemory expects Int in MB
                showMinFreeMemoryDialog = false
            }
        )
    }

    if (showAdjustSwapSizeDialog) {
        SwapSizeDialog(
            currentSize = swapSize,
            maxSize = maxSwapSize,
            onDismiss = { showAdjustSwapSizeDialog = false },
            onConfirm = { newSizeInBytes ->
                vm.setSwapSize(newSizeInBytes)
                showAdjustSwapSizeDialog = false
            }
        )
    }

    // Show loading dialog when swap operation is in progress
    if (isSwapLoading) {
        SwapLoadingDialog(
            logs = swapLogs,
            onDismissRequest = { } // Cannot dismiss while loading
        )
    }
}

// Reusable Composable for clickable setting rows
@Composable
fun ClickableSettingRow(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp), // Padding inside clickable
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            maxLines = 1,
            style = MaterialTheme.typography.bodyLarge
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                maxLines = 1,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown, // Or Icons.Filled.Edit or similar
                contentDescription = "Change $title",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp).padding(start = 4.dp)
            )
        }
    }
}


// Generic Slider Dialog - This is a new reusable Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SliderSettingDialog(
    showDialog: Boolean,
    title: String,
    currentValue: Int,
    valueSuffix: String = "",
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onDismissRequest: () -> Unit,
    onApplyClicked: (Int) -> Unit,
    additionalInfo: String? = null // Optional for min/max/step display like ZRAM
) {
    if (showDialog) {
        var sliderTempValue by remember(currentValue) { mutableStateOf(currentValue.toFloat()) }

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (additionalInfo != null) {
                        Text(
                            text = additionalInfo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Text(
                        "${sliderTempValue.roundToInt()}$valueSuffix",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Slider(
                        value = sliderTempValue,
                        onValueChange = { sliderTempValue = it },
                        valueRange = valueRange,
                        steps = steps,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors( // Consistent colors
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onApplyClicked(sliderTempValue.roundToInt())
                        // onDismissRequest() // Dialog will be closed by the caller
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("APPLY", fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissRequest,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("CANCEL", fontWeight = FontWeight.Medium)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        )
    }
}


@Composable
fun ZramSizeDialog(
    currentSize: Long,
    maxSize: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var sliderValueInBytes by remember(currentSize) { mutableFloatStateOf(currentSize.toFloat()) }

    val minZramSizeMB = 128
    val minZramSizeBytes = minZramSizeMB * 1024L * 1024L
    val stepSizeMB = 128
    val stepSizeBytes = stepSizeMB * 1024L * 1024L

    LaunchedEffect(currentSize, maxSize, minZramSizeBytes, stepSizeBytes) {
        val initialSteps = ((currentSize.toFloat() - minZramSizeBytes.toFloat()) / stepSizeBytes.toFloat())
            .coerceAtLeast(0f)
            .roundToInt()
        val coercedInitialValue = (minZramSizeBytes + initialSteps * stepSizeBytes).toFloat()
            .coerceIn(minZramSizeBytes.toFloat(), maxSize.toFloat())
        sliderValueInBytes = coercedInitialValue
    }

    val currentSizeMBDisplay = sliderValueInBytes.toLong() / (1024 * 1024)
    val maximumSizeMBDisplay = maxSize / (1024 * 1024)
    val minimumSizeMBDisplay = minZramSizeBytes / (1024 * 1024)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Adjust ZRAM Size",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column {
                Text(
                    text = "${currentSizeMBDisplay}MB (Min: ${minimumSizeMBDisplay}MB, Max: ${maximumSizeMBDisplay}MB, Step: ${stepSizeMB}MB)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Slider(
                    value = sliderValueInBytes,
                    onValueChange = { newValueFromSlider ->
                        val stepsCount = ((newValueFromSlider - minZramSizeBytes.toFloat()) / stepSizeBytes.toFloat())
                            .coerceAtLeast(0f)
                            .roundToInt()
                        val newSnappedValue = (minZramSizeBytes + stepsCount * stepSizeBytes).toFloat()
                        sliderValueInBytes = newSnappedValue.coerceIn(minZramSizeBytes.toFloat(), maxSize.toFloat())
                    },
                    valueRange = minZramSizeBytes.toFloat()..maxSize.toFloat(),
                    steps = if (maxSize > minZramSizeBytes && stepSizeBytes > 0) {
                        ((maxSize - minZramSizeBytes) / stepSizeBytes).toInt().coerceAtLeast(0) - 1
                    } else {
                        0
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(sliderValueInBytes.toLong()) }) {
                Text("OK", fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", fontWeight = FontWeight.Medium)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 6.dp
    )
}

// Extracted Compression Algorithm Dialog to its own Composable for clarity
@Composable
fun CompressionAlgorithmDialog(
    compressionAlgorithms: List<String>,
    currentCompression: String,
    onDismiss: () -> Unit,
    onAlgorithmSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Select Compression Algorithm",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column {
                compressionAlgorithms.chunked(2).forEach { rowAlgos ->
                    Row(Modifier.fillMaxWidth()) {
                        rowAlgos.forEach { algo ->
                            Row(
                                Modifier
                                    .weight(1f)
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable { onAlgorithmSelected(algo) }
                                    .padding(vertical = 12.dp, horizontal = 8.dp)
                                    .then(if (algo == currentCompression) Modifier.border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary), MaterialTheme.shapes.medium) else Modifier),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (algo == currentCompression),
                                    onClick = null, // Click handled by parent Row
                                    modifier = Modifier.size(20.dp),
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary,
                                        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = algo,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    color = if (algo == currentCompression) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if (rowAlgos.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("CANCEL", fontWeight = FontWeight.Medium)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 6.dp
    )
}

@Composable
fun SwapSizeDialog(
    currentSize: Long,
    maxSize: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var sliderValueInBytes by remember(currentSize) { mutableFloatStateOf(currentSize.toFloat()) }

    val minSwapSizeBytes = 0L // Allow disabling swap entirely
    val stepSizeMB = 64
    val stepSizeBytes = stepSizeMB * 1024L * 1024L

    LaunchedEffect(currentSize, maxSize, minSwapSizeBytes, stepSizeBytes) {
        // Handle the case where current size is 0 (disabled)
        if (currentSize == 0L) {
            sliderValueInBytes = 0f
        } else {
            val initialSteps = ((currentSize.toFloat() - stepSizeBytes.toFloat()) / stepSizeBytes.toFloat())
                .coerceAtLeast(0f)
                .roundToInt()
            val coercedInitialValue = (stepSizeBytes + initialSteps * stepSizeBytes).toFloat()
                .coerceIn(0f, maxSize.toFloat())
            sliderValueInBytes = coercedInitialValue
        }
    }

    val currentSizeMBDisplay = sliderValueInBytes.toLong() / (1024 * 1024)
    val maximumSizeMBDisplay = maxSize / (1024 * 1024)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Adjust Swap Size",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column {
                Text(
                    text = if (currentSizeMBDisplay == 0L) {
                        "Disabled (Max: ${maximumSizeMBDisplay}MB, Step: ${stepSizeMB}MB)"
                    } else {
                        "${currentSizeMBDisplay}MB (Max: ${maximumSizeMBDisplay}MB, Step: ${stepSizeMB}MB)"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Slider(
                    value = sliderValueInBytes,
                    onValueChange = { newValueFromSlider ->
                        if (newValueFromSlider <= stepSizeBytes.toFloat() / 2) {
                            // Snap to 0 (disabled) if close to minimum
                            sliderValueInBytes = 0f
                        } else {
                            // Snap to step increments
                            val stepsCount = ((newValueFromSlider - stepSizeBytes.toFloat()) / stepSizeBytes.toFloat())
                                .coerceAtLeast(0f)
                                .roundToInt() + 1
                            val newSnappedValue = (stepsCount * stepSizeBytes).toFloat()
                            sliderValueInBytes = newSnappedValue.coerceIn(0f, maxSize.toFloat())
                        }
                    },
                    valueRange = 0f..maxSize.toFloat(),
                    steps = if (maxSize > stepSizeBytes && stepSizeBytes > 0) {
                        ((maxSize) / stepSizeBytes).toInt().coerceAtLeast(0)
                    } else {
                        0
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(sliderValueInBytes.toLong()) }) {
                Text("OK", fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", fontWeight = FontWeight.Medium)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 6.dp
    )
}

@Composable
fun SwapLoadingDialog(
    logs: List<String>,
    onDismissRequest: () -> Unit
) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new logs are added
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Configuring Swap...",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Swap operation in progress. Please wait...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (logs.isNotEmpty()) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Text(
                        text = "Live Logs:",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Scrollable logs with maximum height
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(MaterialTheme.shapes.small)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                MaterialTheme.shapes.small
                            ),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(logs) { log ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = log,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Scroll to see more logs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {}, // Empty to hide button
        dismissButton = {}, // Empty to hide button
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 8.dp
    )
}
