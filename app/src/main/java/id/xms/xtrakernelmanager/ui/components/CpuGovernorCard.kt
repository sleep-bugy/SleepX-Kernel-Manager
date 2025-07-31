package id.xms.xtrakernelmanager.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
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
import androidx.compose.ui.draw.rotate
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

    var showGovernorDialogForCluster by remember { mutableStateOf<String?>(null) }
    var showFreqDialogForCluster by remember { mutableStateOf<String?>(null) }

    // State untuk mengontrol apakah card di-expand atau tidak
    var isExpanded by remember { mutableStateOf(false) } // Defaultnya di-collapse

    // Animasi untuk rotasi ikon dropdown
    val rotationAngle by animateFloatAsState(targetValue = if (isExpanded) 0f else -180f, label = "DropdownRotation")


    GlassCard(blur) {
        Column(
            Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp) // Sesuaikan padding vertikal
                .fillMaxWidth()
        ) {
            // Baris untuk judul dan tombol expand/collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded } // Klik baris untuk toggle
                    .padding(vertical = 8.dp), // Padding untuk area klik yang lebih baik
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween // Agar judul dan ikon terpisah
            ) {
                Text("CPU Control", style = MaterialTheme.typography.titleLarge)
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse CPU Control" else "Expand CPU Control",
                    modifier = Modifier.rotate(if (isExpanded) 0f else -0f) // Bisa juga pakai rotationAngle jika ingin animasi lebih kompleks
                )
            }

            // Konten yang bisa di-expand/collapse
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(4.dp)) // Sedikit spasi setelah judul
                    HorizontalDivider() // Garis pemisah
                    Spacer(modifier = Modifier.height(12.dp))


                    // Kondisi loading utama
                    if (availableGovernors.isEmpty() && clusters.isNotEmpty() &&
                        showGovernorDialogForCluster == null && showFreqDialogForCluster == null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Loading CPU data...")
                        }
                    } else if (clusters.isNotEmpty()) {
                        clusters.forEachIndexed { index, clusterName ->
                            val currentGovernor by vm.getCpuGov(clusterName)
                                .collectAsState() // Hapus initial jika ViewModel sudah benar
                            val currentFreqPair by vm.getCpuFreq(clusterName)
                                .collectAsState() // Hapus initial jika ViewModel sudah benar
                            val availableFrequenciesForCluster by vm.getAvailableCpuFrequencies(
                                clusterName
                            ).collectAsState() // Hapus initial

                            Text(
                                text = clusterName.replaceFirstChar { it.titlecase() },
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 4.dp, top = if (index > 0) 8.dp else 0.dp) // Tambah padding atas jika bukan item pertama
                            )

                            // Pengaturan Governor
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Governor",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .clickable(enabled = currentGovernor != "..." && currentGovernor != "Error" && availableGovernors.isNotEmpty()) {
                                            showGovernorDialogForCluster = clusterName
                                        }
                                        .padding(vertical = 4.dp, horizontal = 8.dp),
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
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Change Governor for $clusterName",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Pengaturan Frekuensi
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Frequency",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .clickable(
                                            enabled = availableFrequenciesForCluster.isNotEmpty() &&
                                                    !(currentFreqPair.first == 0 && currentFreqPair.second == 0 && availableFrequenciesForCluster.isEmpty()) &&
                                                    !(currentFreqPair.first == 0 && currentFreqPair.second == -1) // Cek kondisi error dari VM
                                        ) {
                                            if (availableFrequenciesForCluster.isNotEmpty()) {
                                                showFreqDialogForCluster = clusterName
                                            }
                                        }
                                        .padding(vertical = 4.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    val freqText = when {
                                        currentGovernor == "..." || currentGovernor == "Error" -> currentGovernor
                                        currentFreqPair.first == 0 && currentFreqPair.second == 0 && availableFrequenciesForCluster.isEmpty() -> "Loading..."
                                        currentFreqPair.first == 0 && currentFreqPair.second == -1 -> "Error" // Tampilkan error jika dari VM
                                        else -> "${currentFreqPair.first / 1000} - ${currentFreqPair.second / 1000} MHz"
                                    }
                                    Text(
                                        text = freqText,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Change Frequency for $clusterName",
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
                                val systemMinFreq =
                                    availableFrequenciesForCluster.firstOrNull()
                                        ?: currentFreqPair.first
                                val systemMaxFreq =
                                    availableFrequenciesForCluster.lastOrNull()
                                        ?: currentFreqPair.second

                                if (systemMinFreq in 0..<systemMaxFreq && systemMaxFreq > 0) { // Pastikan frekuensi valid
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
                                        println("Cannot show frequency dialog for $clusterName: Invalid system frequency range ($systemMinFreq - $systemMaxFreq). Available: ${availableFrequenciesForCluster.joinToString()}")
                                        showFreqDialogForCluster = null
                                    }
                                }
                            }

                            if (index < clusters.size - 1) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    } else if (clusters.isEmpty()) {
                        Text(
                            "No CPU clusters found.",
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Text(
                            "CPU data might be partially loaded or an issue occurred.",
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

// ... (GovernorSelectionDialog dan FrequencySelectionDialog tetap sama)
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
        title = { Text("Select Governor for ${clusterName.replaceFirstChar { it.titlecase() }}") },
        text = {
            Column {
                availableGovernors.forEach { governor ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onGovernorSelected(governor) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (governor == currentSelectedGovernor),
                            onClick = { onGovernorSelected(governor) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(governor)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
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
        LaunchedEffect(Unit) {
            onDismiss()
        }
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

    // Efek untuk memastikan sliderMinValue tidak melebihi sliderMaxValue saat nilai awal di-set atau frek berubah
    LaunchedEffect(currentMinFreq, currentMaxFreq, minSystemFreq, maxSystemFreq, allAvailableFrequencies) {
        val newInitialMin = findClosestFrequency(
            currentMinFreq.coerceIn(minSystemFreq, maxSystemFreq),
            allAvailableFrequencies
        ).toFloat()
        val newInitialMax = findClosestFrequency(
            currentMaxFreq.coerceIn(minSystemFreq, maxSystemFreq),
            allAvailableFrequencies
        ).toFloat()

        sliderMinValue = newInitialMin.coerceAtMost(newInitialMax) // Pastikan min <= max
        sliderMaxValue = newInitialMax.coerceAtLeast(newInitialMin) // Pastikan max >= min
    }


    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Frequency for ${clusterName.replaceFirstChar { it.titlecase() }}") },
        text = {
            Column {
                Text("Min: ${sliderMinValue.roundToInt() / 1000} MHz", fontSize = 14.sp)
                Slider(
                    value = sliderMinValue,
                    onValueChange = { newValue ->
                        sliderMinValue = newValue.coerceIn(minSystemFreq.toFloat(), sliderMaxValue) // Batasi dengan nilai max saat ini
                    },
                    valueRange = minSystemFreq.toFloat()..maxSystemFreq.toFloat(),
                    steps = if (allAvailableFrequencies.size > 1) allAvailableFrequencies.size - 2 else 0,
                    onValueChangeFinished = {
                        // Snap ke frekuensi terdekat yang valid
                        sliderMinValue = findClosestFrequency(sliderMinValue.roundToInt(), allAvailableFrequencies).toFloat()
                        // Pastikan min tidak melebihi max setelah snapping
                        if (sliderMinValue > sliderMaxValue) {
                            sliderMinValue = sliderMaxValue // Atau bisa juga set sliderMaxValue ke sliderMinValue
                        }
                    }
                )
                Spacer(Modifier.height(16.dp))
                Text("Max: ${sliderMaxValue.roundToInt() / 1000} MHz", fontSize = 14.sp)
                Slider(
                    value = sliderMaxValue,
                    onValueChange = { newValue ->
                        sliderMaxValue = newValue.coerceIn(sliderMinValue, maxSystemFreq.toFloat()) // Batasi dengan nilai min saat ini
                    },
                    valueRange = minSystemFreq.toFloat()..maxSystemFreq.toFloat(),
                    steps = if (allAvailableFrequencies.size > 1) allAvailableFrequencies.size - 2 else 0,
                    onValueChangeFinished = {
                        // Snap ke frekuensi terdekat yang valid
                        sliderMaxValue = findClosestFrequency(sliderMaxValue.roundToInt(), allAvailableFrequencies).toFloat()
                        // Pastikan max tidak kurang dari min setelah snapping
                        if (sliderMaxValue < sliderMinValue) {
                            sliderMaxValue = sliderMinValue // Atau bisa juga set sliderMinValue ke sliderMaxValue
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Selected: ${sliderMinValue.roundToInt() / 1000} - ${sliderMaxValue.roundToInt() / 1000} MHz",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val finalMin = findClosestFrequency(sliderMinValue.roundToInt(), allAvailableFrequencies)
                val finalMax = findClosestFrequency(sliderMaxValue.roundToInt(), allAvailableFrequencies)

                // Pastikan finalMin tidak lebih besar dari finalMax
                if (finalMin <= finalMax) {
                    onFrequencySelected(finalMin, finalMax)
                } else {
                    // Jika logika snapping membuat min > max, mungkin kirim min, min atau max, max
                    onFrequencySelected(finalMin, finalMin) // atau (finalMax, finalMax) atau biarkan (finalMin, finalMax) dan ViewModel yang menangani
                }
            }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun findClosestFrequency(target: Int, availableFrequencies: List<Int>): Int {
    if (availableFrequencies.isEmpty()) return target.coerceAtLeast(0) // Pastikan tidak negatif jika kosong
    return availableFrequencies.minByOrNull { abs(it - target) } ?: target.coerceAtLeast(0)
}
