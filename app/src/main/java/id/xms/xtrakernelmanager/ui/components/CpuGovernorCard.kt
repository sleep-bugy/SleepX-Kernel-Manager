package id.xms.xtrakernelmanager.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.graphics.TransformOrigin
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
        targetValue = if (isExpanded) 0f else -180f,
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
                    "CPU Control",
                    style = MaterialTheme.typography.titleLarge,
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse CPU Control" else "Expand CPU Control",
                    modifier = Modifier.rotate(rotationAngle)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = slideInVertically(
                    initialOffsetY = { -it / 2 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(
                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                ) + scaleIn(
                    transformOrigin = TransformOrigin(0.5f, 0f),
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ),
                exit = slideOutVertically(
                    targetOffsetY = { -it / 2 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + shrinkVertically(
                    shrinkTowards = Alignment.Top,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeOut(
                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                ) + scaleOut(
                    transformOrigin = TransformOrigin(0.5f, 0f),
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            ) {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (availableGovernors.isEmpty() && clusters.isNotEmpty() &&
                        showGovernorDialogForCluster == null && showFreqDialogForCluster == null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Loading CPU data...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (clusters.isNotEmpty()) {
                        clusters.forEachIndexed { index, clusterName ->
                            val currentGovernor by vm.getCpuGov(clusterName).collectAsState()
                            val currentFreqPair by vm.getCpuFreq(clusterName).collectAsState()
                            val availableFrequenciesForCluster by vm.getAvailableCpuFrequencies(clusterName).collectAsState()

                            Text(
                                text = clusterName.replaceFirstChar { it.titlecase() },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = MaterialTheme.colorScheme.tertiary,
                                ),
                                modifier = Modifier.padding(
                                    bottom = 8.dp,
                                    top = if (index > 0) 16.dp else 0.dp
                                )
                            )

                            // Pengaturan Governor
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable(enabled = currentGovernor != "..." && currentGovernor != "Error" && availableGovernors.isNotEmpty()) {
                                        showGovernorDialogForCluster = clusterName
                                    }
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Governor",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = if (currentGovernor == "..." || currentGovernor == "Error") currentGovernor else currentGovernor,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Change Governor for $clusterName",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }


                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable(
                                        enabled = availableFrequenciesForCluster.isNotEmpty() &&
                                                !(currentFreqPair.first == 0 && currentFreqPair.second == 0 && availableFrequenciesForCluster.isEmpty()) &&
                                                !(currentFreqPair.first == 0 && currentFreqPair.second == -1)
                                    ) {
                                        if (availableFrequenciesForCluster.isNotEmpty()) {
                                            showFreqDialogForCluster = clusterName
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Frequency",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    val freqText = when {
                                        currentGovernor == "..." || currentGovernor == "Error" -> currentGovernor
                                        currentFreqPair.first == 0 && currentFreqPair.second == 0 && availableFrequenciesForCluster.isEmpty() -> "Loading..."
                                        currentFreqPair.first == 0 && currentFreqPair.second == -1 -> "Error"
                                        else -> "${currentFreqPair.first / 1000} - ${currentFreqPair.second / 1000} MHz"
                                    }
                                    Text(
                                        text = freqText,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Change Frequency for $clusterName",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }


                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable {
                                        showCoreDialogForCluster = clusterName
                                    }
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Core Status",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = "${coreStates.count { it }}/${coreStates.size} Cores Online",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Change Core Status for $clusterName",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            if (showGovernorDialogForCluster == clusterName) {
                                GovernorSelectionDialog(
                                    clusterName = clusterName,
                                    availableGovernors = availableGovernors,
                                    currentSelectedGovernor = currentGovernor,
                                    onGovernorSelected = { selectedGov ->
                                        vm.setCpuGov(clusterName, selectedGov)
                                        showGovernorDialogForCluster = null
                                    },
                                    onDismiss = { showGovernorDialogForCluster = null }
                                )
                            }

                            if (showFreqDialogForCluster == clusterName && availableFrequenciesForCluster.isNotEmpty()) {
                                val systemMinFreq = availableFrequenciesForCluster.firstOrNull() ?: currentFreqPair.first
                                val systemMaxFreq = availableFrequenciesForCluster.lastOrNull() ?: currentFreqPair.second

                                if (systemMinFreq < systemMaxFreq && systemMaxFreq > 0) {
                                    FrequencySelectionDialog(
                                        clusterName = clusterName,
                                        currentMinFreq = currentFreqPair.first,
                                        currentMaxFreq = currentFreqPair.second,
                                        minSystemFreq = systemMinFreq,
                                        maxSystemFreq = systemMaxFreq,
                                        allAvailableFrequencies = availableFrequenciesForCluster,
                                        onFrequencySelected = { newMin, newMax ->
                                            vm.setCpuFreq(clusterName, newMin, newMax)
                                            showFreqDialogForCluster = null
                                        },
                                        onDismiss = { showFreqDialogForCluster = null }
                                    )
                                } else {
                                    LaunchedEffect(clusterName) {
                                        println("CpuGovernorCard: Cannot show frequency dialog for $clusterName. Invalid system frequency range (SystemMin: $systemMinFreq, SystemMax: $systemMaxFreq). Current Freq Pair: ${currentFreqPair.first}-${currentFreqPair.second}. Available: ${availableFrequenciesForCluster.joinToString()}")
                                        showFreqDialogForCluster = null
                                    }
                                }
                            }

                            if (showCoreDialogForCluster == clusterName) {
                                CoreStatusDialog(
                                    clusterName = clusterName,
                                    coreStates = coreStates,
                                    onCoreToggled = { coreId ->
                                        vm.toggleCore(coreId)
                                    },
                                    onDismiss = { showCoreDialogForCluster = null }
                                )
                            }

                            if (index < clusters.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 16.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            }
                        }
                    } else if (clusters.isEmpty()) {
                        Text(
                            "No CPU clusters found. Ensure the kernel provides this information.",
                            modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    } else {
                        Text(
                            "CPU data could not be fully loaded or is unavailable.",
                            modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
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
        title = { Text("Select Governor", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
                Text(
                    clusterName.replaceFirstChar { it.titlecase() },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp, top = 4.dp)
                )
                availableGovernors.forEach { governor ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onGovernorSelected(governor) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (governor == currentSelectedGovernor),
                            onClick = { onGovernorSelected(governor) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            governor,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (governor == currentSelectedGovernor) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
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
        title = { Text("Set Frequency", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
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
            Button(
                onClick = {
                    val finalMin = findClosestFrequency(sliderMinValue.roundToInt(), allAvailableFrequencies)
                    val finalMax = findClosestFrequency(sliderMaxValue.roundToInt(), allAvailableFrequencies)
                    if (finalMin <= finalMax) {
                        onFrequencySelected(finalMin, finalMax)
                    } else {
                        onFrequencySelected(finalMin, finalMin)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("APPLY")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("CANCEL")
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Core Status", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
                Text(
                    clusterName.replaceFirstChar { it.titlecase() },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp, top = 4.dp)
                )
                coreStates.forEachIndexed { index, isOnline ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onCoreToggled(index) }
                            .padding(vertical = 8.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isOnline,
                            onClick = { onCoreToggled(index) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = "Core $index",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("DONE", fontWeight = FontWeight.Medium)
            }
        }
    )
}

private fun findClosestFrequency(target: Int, availableFrequencies: List<Int>): Int {
    if (availableFrequencies.isEmpty()) return target.coerceAtLeast(0)
    if (target in availableFrequencies) return target
    return availableFrequencies.minByOrNull { abs(it - target) } ?: target.coerceAtLeast(0)
}