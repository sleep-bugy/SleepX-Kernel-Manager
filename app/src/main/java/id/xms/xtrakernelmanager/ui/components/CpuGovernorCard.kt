package id.xms.xtrakernelmanager.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.xms.xtrakernelmanager.viewmodel.TuningViewModel
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun CpuGovernorCard(
    vm: TuningViewModel,
    blur: Boolean,
) {
    val clusters = vm.cpuClusters
    val availableGovernors by vm.generalAvailableCpuGovernors.collectAsState()
    val coreStates by vm.coreStates.collectAsState()

    var showGovernorDialogForCluster by remember { mutableStateOf<String?>(null) }
    var showFreqDialogForCluster by remember { mutableStateOf<String?>(null) }
    var showCoreDialogForCluster by remember { mutableStateOf<String?>(null) }

    var isExpanded by remember { mutableStateOf(false) }

    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "DropdownRotation"
    )

    SuperGlassCard(
        modifier = Modifier.fillMaxWidth(),
        glassIntensity = if (blur) GlassIntensity.Light else GlassIntensity.Light
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with expand/collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "CPU Control",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.rotate(rotationAngle),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "Configure CPU governor and frequency settings for each cluster",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AnimatedVisibility(
                visible = isExpanded,
                enter = slideInVertically() + expandVertically() + fadeIn(),
                exit = slideOutVertically() + shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (availableGovernors.isEmpty() && clusters.isNotEmpty()) {
                        SuperGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            glassIntensity = GlassIntensity.Light
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Loading CPU data...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (clusters.isNotEmpty()) {
                        clusters.forEachIndexed { _, clusterName ->
                            CpuClusterCard(
                                clusterName = clusterName,
                                vm = vm,
                                onGovernorClick = { showGovernorDialogForCluster = clusterName },
                                onFrequencyClick = { showFreqDialogForCluster = clusterName },
                                onCoreClick = { showCoreDialogForCluster = clusterName }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showGovernorDialogForCluster != null) {
        GovernorSelectionDialog(
            clusterName = showGovernorDialogForCluster!!,
            availableGovernors = availableGovernors,
            currentSelectedGovernor = vm.getCpuGov(showGovernorDialogForCluster!!).collectAsState().value,
            onGovernorSelected = { selectedGov ->
                vm.setCpuGov(showGovernorDialogForCluster!!, selectedGov)
                showGovernorDialogForCluster = null
            },
            onDismiss = { showGovernorDialogForCluster = null }
        )
    }

    if (showFreqDialogForCluster != null) {
        val clusterName = showFreqDialogForCluster!!
        val availableFrequenciesForCluster by vm.getAvailableCpuFrequencies(clusterName).collectAsState()
        val currentFreqPair by vm.getCpuFreq(clusterName).collectAsState()

        // Show dialog even if data is not perfect - let user see what's happening
        if (availableFrequenciesForCluster.isNotEmpty()) {
            val systemMinFreq = availableFrequenciesForCluster.minOrNull() ?: currentFreqPair.first
            val systemMaxFreq = availableFrequenciesForCluster.maxOrNull() ?: currentFreqPair.second

            // More lenient condition - allow dialog to show even with imperfect data
            if (systemMinFreq > 0 && systemMaxFreq > 0 && systemMinFreq <= systemMaxFreq) {
                FrequencySelectionDialog(
                    clusterName = clusterName,
                    currentMinFreq = currentFreqPair.first.coerceAtLeast(systemMinFreq),
                    currentMaxFreq = currentFreqPair.second.coerceAtMost(systemMaxFreq),
                    minSystemFreq = systemMinFreq,
                    maxSystemFreq = systemMaxFreq,
                    allAvailableFrequencies = availableFrequenciesForCluster.sorted(),
                    onFrequencySelected = { newMin, newMax ->
                        vm.setCpuFreq(clusterName, newMin, newMax)
                        showFreqDialogForCluster = null
                    },
                    onDismiss = { showFreqDialogForCluster = null }
                )
            } else {
                AlertDialog(
                    onDismissRequest = { showFreqDialogForCluster = null },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "Frequency Error",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                    text = {
                        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                            Text("Cannot adjust frequency for $clusterName.\nInvalid frequency range: $systemMinFreq - $systemMaxFreq MHz")
                        }
                    },
                    confirmButton = {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp, end = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showFreqDialogForCluster = null }) {
                                Text("OK")
                            }
                        }
                    }
                )
            }
        } else {
            AlertDialog(
                onDismissRequest = { showFreqDialogForCluster = null },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Loading Frequencies",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                text = {
                    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                        if (currentFreqPair.first == 0 && currentFreqPair.second == 0) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                Text("Loading frequency data for $clusterName...")
                            }
                        } else {
                            Text("No available frequencies found for $clusterName.\nCurrent: ${currentFreqPair.first/1000} - ${currentFreqPair.second/1000} MHz")
                        }
                    }
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp, end = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showFreqDialogForCluster = null }) {
                            Text("OK")
                        }
                    }
                }
            )
        }
    }

    if (showCoreDialogForCluster != null) {
        CoreStatusDialog(
            clusterName = showCoreDialogForCluster!!,
            coreStates = coreStates,
            onCoreToggled = { coreId ->
                vm.toggleCore(coreId)
            },
            onDismiss = { showCoreDialogForCluster = null }
        )
    }
}

@Composable
private fun GovernorSelectionDialog(
    clusterName: String,
    availableGovernors: List<String>,
    currentSelectedGovernor: String,
    onGovernorSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Select Governor for ${clusterName.replaceFirstChar { it.titlecase() }}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        text = {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    availableGovernors.sorted().forEach { governor ->
                        val isSelected = governor == currentSelectedGovernor
                        androidx.compose.material3.FilledTonalButton(
                            onClick = { onGovernorSelected(governor) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isSelected) Color.Red else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(16.dp),
                            enabled = !isSelected
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    governor,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.FilledTonalButton(
                onClick = onDismiss,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun FrequencySelectionDialog(
    clusterName: String,
    currentMinFreq: Int,
    currentMaxFreq: Int,
    minSystemFreq: Int,
    maxSystemFreq: Int,
    allAvailableFrequencies: List<Int>,
    onFrequencySelected: (min: Int, max: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    if (allAvailableFrequencies.isEmpty() || minSystemFreq >= maxSystemFreq) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    var sliderMinValue by remember(currentMinFreq, minSystemFreq, maxSystemFreq, allAvailableFrequencies) {
        mutableFloatStateOf(
            findClosestFrequency(
                currentMinFreq.coerceIn(minSystemFreq, maxSystemFreq),
                allAvailableFrequencies
            ).toFloat()
        )
    }
    var sliderMaxValue by remember(currentMaxFreq, minSystemFreq, maxSystemFreq, allAvailableFrequencies) {
        mutableFloatStateOf(
            findClosestFrequency(
                currentMaxFreq.coerceIn(minSystemFreq, maxSystemFreq),
                allAvailableFrequencies
            ).toFloat()
        )
    }

    LaunchedEffect(currentMinFreq, currentMaxFreq, minSystemFreq, maxSystemFreq, allAvailableFrequencies) {
        val newInitialMin = findClosestFrequency(currentMinFreq.coerceIn(minSystemFreq, maxSystemFreq), allAvailableFrequencies).toFloat()
        val newInitialMax = findClosestFrequency(currentMaxFreq.coerceIn(minSystemFreq, maxSystemFreq), allAvailableFrequencies).toFloat()
        sliderMinValue = newInitialMin.coerceAtMost(newInitialMax)
        sliderMaxValue = newInitialMax.coerceAtLeast(newInitialMin)
    }

    val sliderColors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.primary,
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
        activeTickColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
        inactiveTickColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Set Frequency",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        text = {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text(
                    clusterName.replaceFirstChar { it.titlecase() },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp, top = 4.dp)
                )
                // Min Frequency
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Min Frequency", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "${sliderMinValue.roundToInt() / 1000} MHz",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = sliderMinValue,
                    onValueChange = { newValue ->
                        sliderMinValue = newValue.coerceIn(minSystemFreq.toFloat(), sliderMaxValue)
                    },
                    valueRange = minSystemFreq.toFloat()..maxSystemFreq.toFloat(),
                    steps = if (allAvailableFrequencies.size > 1) (allAvailableFrequencies.size - 2).coerceAtLeast(0) else 0,
                    onValueChangeFinished = {
                        sliderMinValue = findClosestFrequency(sliderMinValue.roundToInt(), allAvailableFrequencies).toFloat()
                        if (sliderMinValue > sliderMaxValue) sliderMinValue = sliderMaxValue
                    },
                    colors = sliderColors
                )
                Spacer(Modifier.height(24.dp))
                // Max Frequency
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Max Frequency", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "${sliderMaxValue.roundToInt() / 1000} MHz",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = sliderMaxValue,
                    onValueChange = { newValue ->
                        sliderMaxValue = newValue.coerceIn(sliderMinValue, maxSystemFreq.toFloat())
                    },
                    valueRange = minSystemFreq.toFloat()..maxSystemFreq.toFloat(),
                    steps = if (allAvailableFrequencies.size > 1) (allAvailableFrequencies.size - 2).coerceAtLeast(0) else 0,
                    onValueChangeFinished = {
                        sliderMaxValue = findClosestFrequency(sliderMaxValue.roundToInt(), allAvailableFrequencies).toFloat()
                        if (sliderMaxValue < sliderMinValue) sliderMaxValue = sliderMinValue
                    },
                    colors = sliderColors
                )
                Spacer(Modifier.height(16.dp))
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                androidx.compose.material3.FilledTonalButton(
                    onClick = {
                        val finalMin = findClosestFrequency(sliderMinValue.roundToInt(), allAvailableFrequencies)
                        val finalMax = findClosestFrequency(sliderMaxValue.roundToInt(), allAvailableFrequencies)
                        if (finalMin <= finalMax) {
                            onFrequencySelected(finalMin, finalMax)
                        } else {
                            onFrequencySelected(finalMin, finalMin)
                        }
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Apply",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("APPLY")
                }
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.material3.FilledTonalButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CANCEL")
                }
            }
        }
    )
}

@Composable
private fun CoreStatusDialog(
    clusterName: String,
    coreStates: List<Boolean>,
    onCoreToggled: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val greenOnline = Color(0xFF4CAF50)
    val redOffline = Color(0xFFF44336)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Core Status",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        text = {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text(
                    clusterName.replaceFirstChar { it.titlecase() },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp, top = 4.dp)
                )
                coreStates.forEachIndexed { index, isOnline ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onCoreToggled(index) }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onCoreToggled(index) },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isOnline) greenOnline else redOffline,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .size(24.dp)
                                .padding(0.dp),
                        ) {}
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = "Core $index",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = if (isOnline) "Online" else "Offline",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (isOnline) greenOnline else redOffline,
                                fontWeight = FontWeight.SemiBold
                            ),
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.FilledTonalButton(
                onClick = onDismiss,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Done",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("DONE")
            }
        }
    )
}

private fun findClosestFrequency(target: Int, availableFrequencies: List<Int>): Int {
    if (availableFrequencies.isEmpty()) return target.coerceAtLeast(0)
    if (target in availableFrequencies) return target
    return availableFrequencies.minByOrNull { abs(it - target) } ?: target.coerceAtLeast(0)
}
@Composable
fun CpuClusterCard(
    clusterName: String,
    vm: TuningViewModel,
    onGovernorClick: () -> Unit,
    onFrequencyClick: () -> Unit,
    onCoreClick: () -> Unit
) {
    val currentGovernor by vm.getCpuGov(clusterName).collectAsState()
    val currentFreqPair by vm.getCpuFreq(clusterName).collectAsState()
    val availableFrequenciesForCluster by vm.getAvailableCpuFrequencies(clusterName).collectAsState()
    val coreStates by vm.coreStates.collectAsState()

    val clusterColors = when (clusterName) {
        "cpu0" -> Color(0xFF20BD9D) // Teal
        "cpu1" -> Color(0xFFFFCA28) // Amber
        "cpu2" -> Color(0xFF891637) // Dark Red
        "cpu3" -> Color(0xFF29B6F6) // Blue
        "cpu4" -> Color(0xFF66BB6A) // Green
        "cpu5" -> Color(0xFFFFA726) // Orange
        "cpu6" -> Color(0xFFEC407A) // Red Pink
        "cpu7" -> Color(0xFFDB5430) // Deep Orange
        else -> Color(0xFF9C27B0)   // Purple fallback
    }

    SuperGlassCard(
        modifier = Modifier.fillMaxWidth(),
        glassIntensity = GlassIntensity.Light
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                color = clusterColors.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "Cluster Icon",
                            tint = clusterColors,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = clusterName.uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                        ),
                        color = clusterColors
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- KONTROL DENGAN LAYOUT PADAT YANG BARU ---
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Governor Control
                val govIsLoading = currentGovernor == "..." || currentGovernor == "Error"
                CompactControlItem(
                    title = "Governor",
                    icon = Icons.Default.Tune,
                    value = currentGovernor,
                    isLoading = govIsLoading,
                    themeColor = clusterColors,
                    enabled = !govIsLoading,
                    onClick = onGovernorClick
                )

                // Frequency Control
                val freqText = when {
                    currentGovernor == "..." -> "..."
                    currentGovernor == "Error" -> "Error"
                    currentFreqPair.first == 0 && currentFreqPair.second == 0 -> "Loading..."
                    else -> "${currentFreqPair.first / 1000} - ${currentFreqPair.second / 1000} MHz"
                }
                val freqIsLoading = freqText == "Loading..." || freqText == "..." || freqText == "Error"
                CompactControlItem(
                    title = "Frequency",
                    icon = Icons.Default.Speed,
                    value = freqText,
                    isLoading = freqIsLoading,
                    themeColor = clusterColors,
                    enabled = availableFrequenciesForCluster.isNotEmpty() && !freqIsLoading,
                    onClick = onFrequencyClick
                )

                // Core Status Control
                CompactControlItem(
                    title = "Core Status",
                    icon = Icons.Default.Memory,
                    value = "${coreStates.count { it }}/${coreStates.size} Online",
                    isLoading = false,
                    themeColor = clusterColors,
                    enabled = true,
                    onClick = onCoreClick
                )
            }
        }
    }
}


@Composable
private fun CompactControlItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    isLoading: Boolean,
    themeColor: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    SuperGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        glassIntensity = GlassIntensity.Light
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (enabled) themeColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = themeColor
                    )
                } else {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) themeColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            // Chevron Icon
            if (enabled) {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Open Settings",
                    tint = themeColor.copy(alpha = 0.7f),
                    modifier = Modifier.rotate(-90f)
                )
            }
        }
    }
}

@Composable
private fun ControlSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    isLoading: Boolean,
    themeColor: Color,
    onClick: () -> Unit,
    enabled: Boolean
) {
    SuperGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() },
        glassIntensity = GlassIntensity.Light
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon with themed background
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = if (enabled) themeColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) themeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )

                if (isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = themeColor
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (enabled) themeColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Arrow indicator
            if (enabled) {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Expand",
                    tint = themeColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}