package id.xms.xtrakernelmanager.ui.components

import android.util.Log // Tambahkan import Log jika belum ada
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.xms.xtrakernelmanager.viewmodel.TuningViewModel
// Pastikan GlassCard diimpor jika ini adalah komponen custom Anda
// import id.xms.xtrakernelmanager.ui.components.GlassCard
import kotlin.math.roundToInt

@Composable
fun GpuControlCard(
    modifier: Modifier = Modifier,
    tuningViewModel: TuningViewModel = hiltViewModel(),
    blur: Boolean
) {
    val availableGpuGovernors by tuningViewModel.availableGpuGovernors.collectAsState()
    val currentGpuGovernor by tuningViewModel.currentGpuGovernor.collectAsState()

    // Mengasumsikan StateFlow dari ViewModel ini memberikan nilai dalam kHz
    val availableGpuFrequenciesFromVM by tuningViewModel.availableGpuFrequencies.collectAsState()
    Log.d("GpuControlCard", "RAW availableGpuFrequencies FROM VM (LIKELY ALREADY MHz): $availableGpuFrequenciesFromVM")

    val currentMinGpuFreqMHz by tuningViewModel.currentGpuMinFreq.collectAsState()
    Log.d("GpuControlCard", "currentMinGpuFreqMHz from VM: $currentMinGpuFreqMHz")
    val currentMaxGpuFreqMHz by tuningViewModel.currentGpuMaxFreq.collectAsState()
    Log.d("GpuControlCard", "currentMaxGpuFreqMHz from VM: $currentMaxGpuFreqMHz")

    // Log untuk debugging nilai yang diterima dari ViewModel
    Log.d("GpuControlCard", "currentMinGpuFreqMHz from VM: $currentMinGpuFreqMHz")
    Log.d("GpuControlCard", "currentMaxGpuFreqMHz from VM: $currentMaxGpuFreqMHz")


    val gpuPowerLevelRange by tuningViewModel.gpuPowerLevelRange.collectAsState()
    val currentGpuPowerLevelValue by tuningViewModel.currentGpuPowerLevel.collectAsState()
    val openGlesDriverState by tuningViewModel.currentOpenGlesDriver.collectAsState()
    val currentGpuRenderer by tuningViewModel.currentGpuRenderer.collectAsState()
    val availableGpuRenderers = remember {
        listOf("Default", "OpenGL", "Vulkan", "ANGLE", "OpenGL (SKIA)", "Vulkan (SKIA)")
    }

    val showRebootDialog by tuningViewModel.showRebootConfirmationDialog.collectAsState()

    var selectedGpuGovernorState by remember(currentGpuGovernor) { mutableStateOf(currentGpuGovernor) }
    var showMinFreqDialog by remember { mutableStateOf(false) }
    var showMaxFreqDialog by remember { mutableStateOf(false) }
    var sliderPosition by remember(currentGpuPowerLevelValue) { mutableFloatStateOf(currentGpuPowerLevelValue) }
    var isExpanded by remember { mutableStateOf(false) }

    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 0f else -180f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "DropdownRotation"
    )

    LaunchedEffect(currentGpuGovernor) {
        if (currentGpuGovernor.isNotBlank() && currentGpuGovernor != "..." && currentGpuGovernor != "Error") {
            selectedGpuGovernorState = currentGpuGovernor
        }
    }


    val availableGpuFrequenciesMHz = remember(availableGpuFrequenciesFromVM) {
        Log.d("GpuControlCard", "Processing availableGpuFrequenciesFromVM: $availableGpuFrequenciesFromVM")
        availableGpuFrequenciesFromVM
            .filter { it > 0 }
            .distinct()
            .sorted()
    }
    Log.d("GpuControlCard", "FINAL availableGpuFrequenciesMHz for UI: $availableGpuFrequenciesMHz")


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
                    text = "GPU Control",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    textAlign = TextAlign.Start
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                    contentDescription = if (isExpanded) "Collapse GPU Control" else "Expand GPU Control",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = slideInVertically(initialOffsetY = { -it / 2 }, animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)) + expandVertically(expandFrom = Alignment.Top, animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) + scaleIn(transformOrigin = TransformOrigin(0.5f, 0f), animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)),
                exit = slideOutVertically(targetOffsetY = { -it / 2 }, animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)) + shrinkVertically(shrinkTowards = Alignment.Top, animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) + scaleOut(transformOrigin = TransformOrigin(0.5f, 0f), animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle(text = "Graphics Driver Information")
                    val openGlEsVersion by tuningViewModel.currentOpenGlesDriver.collectAsState()
                    InfoRow(label = "OpenGL ES Version", value = openGlEsVersion.ifBlank { "N/A" })
                    InfoRow(label = "Vulkan API Version", value = "1.1")

                    Spacer(modifier = Modifier.height(16.dp))

                    GpuRendererControl(
                        currentRenderer = currentGpuRenderer,
                        availableRenderers = availableGpuRenderers,
                        onRendererSelected = { selectedRenderer ->
                            if (selectedRenderer != currentGpuRenderer) {
                                tuningViewModel.userSelectedGpuRenderer(selectedRenderer)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    GpuGovernorControl(
                        availableGovernors = availableGpuGovernors,
                        currentGovernor = selectedGpuGovernorState,
                        onGovernorSelected = { newGov ->
                            selectedGpuGovernorState = newGov
                            tuningViewModel.setGpuGovernor(newGov)
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    SectionTitle(text = "Frequency Control")
                    GpuFrequencyControl(
                        label = "Min Frequency",
                        currentFrequencyMHz = currentMinGpuFreqMHz,
                        availableFrequenciesMHz = availableGpuFrequenciesMHz,
                        onFrequencySelectedMHz = { freqMHz ->
                            tuningViewModel.setGpuMinFrequency(freqMHz)
                        },
                        showDialog = showMinFreqDialog,
                        onShowDialogChange = { showMinFreqDialog = it }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    GpuFrequencyControl(
                        label = "Max Frequency",
                        currentFrequencyMHz = currentMaxGpuFreqMHz,
                        availableFrequenciesMHz = availableGpuFrequenciesMHz.filter { it >= currentMinGpuFreqMHz.coerceAtLeast(0) },
                        onFrequencySelectedMHz = { freqMHz ->
                            tuningViewModel.setGpuMaxFrequency(freqMHz)
                        },
                        showDialog = showMaxFreqDialog,
                        onShowDialogChange = { showMaxFreqDialog = it }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (gpuPowerLevelRange.first < gpuPowerLevelRange.second && (gpuPowerLevelRange.second - gpuPowerLevelRange.first > 0)) {
                        SectionTitle(text = "Power Management")
                        GpuPowerLevelSlider(
                            label = "Power Level",
                            sliderValue = sliderPosition,
                            valueRange = gpuPowerLevelRange,
                            steps = (gpuPowerLevelRange.second - gpuPowerLevelRange.first).toInt().coerceAtLeast(1) - 1,
                            onValueChange = { newValue ->
                                sliderPosition = newValue
                            },
                            onValueChangeFinished = {
                                tuningViewModel.setGpuPowerLevel(sliderPosition)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showRebootDialog) {
        RebootConfirmationDialog(
            onDismiss = { tuningViewModel.cancelRebootConfirmation() },
            onConfirm = { tuningViewModel.confirmAndRebootDevice() }
        )
    }
}


@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
    )
}


@Composable
private fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)),
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.5f)
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GpuRendererControl(
    currentRenderer: String,
    availableRenderers: List<String>,
    onRendererSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Text(
            text = "GPU Renderer",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.weight(1f)
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.weight(1.5f)
        ) {
            OutlinedTextField(
                value = if (currentRenderer.isBlank() || currentRenderer == "..." || currentRenderer == "Error") "Loading..." else currentRenderer,
                onValueChange = {},
                readOnly = true,
                label = { Text("Select Renderer") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (availableRenderers.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No renderers available") },
                        onClick = { expanded = false },
                        enabled = false
                    )
                } else {
                    availableRenderers.forEach { rendererItem ->
                        DropdownMenuItem(
                            text = { Text(rendererItem, style = MaterialTheme.typography.bodyLarge) },
                            onClick = {
                                onRendererSelected(rendererItem)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GpuGovernorControl(
    availableGovernors: List<String>,
    currentGovernor: String,
    onGovernorSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Governor",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.weight(1f)
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.weight(1.5f)
        ) {
            OutlinedTextField(
                value = if (currentGovernor.isBlank() || currentGovernor == "..." || currentGovernor == "Error") "Loading..." else currentGovernor,
                onValueChange = {},
                readOnly = true,
                label = { Text("Select Governor") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (availableGovernors.isEmpty()) {
                    val displayText = when {
                        currentGovernor == "..." && availableGovernors.isEmpty() -> "Loading..."
                        currentGovernor == "Error" -> "Error"
                        currentGovernor.isNotBlank() && availableGovernors.isEmpty() && currentGovernor != "..." -> currentGovernor
                        else -> "N/A"
                    }
                    DropdownMenuItem(
                        text = { Text(displayText) },
                        onClick = { expanded = false },
                        enabled = false
                    )
                } else {
                    availableGovernors.forEach { governor ->
                        DropdownMenuItem(
                            text = { Text(governor, style = MaterialTheme.typography.bodyLarge) },
                            onClick = {
                                onGovernorSelected(governor)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GpuFrequencyControl(
    label: String,
    currentFrequencyMHz: Int, // Diperbarui untuk menerima MHz
    availableFrequenciesMHz: List<Int>, // Diperbarui untuk menerima daftar MHz
    onFrequencySelectedMHz: (Int) -> Unit, // Callback sekarang dengan MHz
    showDialog: Boolean,
    onShowDialogChange: (Boolean) -> Unit
) {
    // Fungsi format sekarang langsung menggunakan nilai MHz
    fun formatMHzToString(freqMHz: Int, isCurrentValue: Boolean = false): String {
        // Untuk nilai saat ini yang ditampilkan di Row, jika 0 atau negatif, dan bukan dari daftar pilihan, tampilkan "N/A"
        if (isCurrentValue && freqMHz <= 0 && !availableFrequenciesMHz.contains(freqMHz)) return "N/A"
        // Untuk item dalam dialog, jika 0 atau negatif (seharusnya tidak terjadi jika daftar difilter),
        // atau untuk nilai saat ini yang valid tapi 0 (mis. belum dimuat), tampilkan "N/A" atau sesuai logika.
        if (freqMHz <= 0) return "N/A" // Atau "Loading..." jika freqMHz adalah nilai awal default
        return "$freqMHz MHz"
    }

    // tempSelected sekarang juga dalam MHz
    var tempSelectedMHz by remember(currentFrequencyMHz, showDialog) { mutableIntStateOf(currentFrequencyMHz) }

    Log.d("GpuFrequencyControl", "Label: $label, currentFrequencyMHz: $currentFrequencyMHz, availableFrequenciesMHz: $availableFrequenciesMHz, showDialog: $showDialog")


    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onShowDialogChange(true) }
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.weight(1f)
        )
        Row(
            modifier = Modifier.weight(1.5f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                // Menggunakan formatMHzToString dengan nilai MHz
                text = formatMHzToString(currentFrequencyMHz, isCurrentValue = true),
                style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary),
                textAlign = TextAlign.End
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Filled.Edit,
                contentDescription = "Edit $label",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { onShowDialogChange(false) },
            title = { Text("Select $label", style = MaterialTheme.typography.headlineSmall) },
            text = {
                if (availableFrequenciesMHz.isEmpty()) {
                    Text("No frequencies available.", style = MaterialTheme.typography.bodyLarge)
                } else {
                    // Pastikan tempSelectedMHz direset dengan benar saat dialog dibuka
                    LaunchedEffect(currentFrequencyMHz, showDialog) {
                        if (showDialog) tempSelectedMHz = currentFrequencyMHz
                    }
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        // Iterasi melalui daftar frekuensi MHz yang sudah diurutkan
                        items(availableFrequenciesMHz) { freqMHz -> // freqMHz sudah dalam MHz
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = (freqMHz == tempSelectedMHz),
                                        onClick = { tempSelectedMHz = freqMHz },
                                        role = androidx.compose.ui.semantics.Role.RadioButton
                                    )
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (freqMHz == tempSelectedMHz),
                                    onClick = { tempSelectedMHz = freqMHz },
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    // Menampilkan nilai MHz secara langsung
                                    text = formatMHzToString(freqMHz),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Kirim nilai tempSelectedMHz (yang sudah dalam MHz)
                        if (tempSelectedMHz != currentFrequencyMHz && availableFrequenciesMHz.isNotEmpty()) {
                            onFrequencySelectedMHz(tempSelectedMHz)
                        }
                        onShowDialogChange(false)
                    },
                    enabled = availableFrequenciesMHz.isNotEmpty()
                ) {
                    Text("Apply", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { onShowDialogChange(false) }) {
                    Text("Cancel", style = MaterialTheme. typography.labelLarge)
                }
            }
        )
    }
}

@Composable
private fun GpuPowerLevelSlider(
    label: String,
    sliderValue: Float,
    valueRange: Pair<Float, Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = sliderValue.roundToInt().toString(),
                style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary),
                textAlign = TextAlign.End,
                modifier = Modifier.weight(0.5f)
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = onValueChange,
            valueRange = valueRange.first..valueRange.second,
            steps = if (steps >= 0) steps else 0,
            onValueChangeFinished = onValueChangeFinished,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
            )
        )
    }
}

@Composable
private fun RebootConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Info, contentDescription = "Information") },
        title = { Text("Restart Required", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Text(
                "Changing the GPU renderer requires a device restart for the changes to take effect. If the renderer doesn't change after reboot, your device may not support this specific selection.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Reboot Now", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Maybe Later", style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}
