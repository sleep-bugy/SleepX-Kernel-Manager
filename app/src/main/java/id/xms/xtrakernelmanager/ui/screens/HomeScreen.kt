package id.xms.xtrakernelmanager.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import id.xms.xtrakernelmanager.R
import id.xms.xtrakernelmanager.ui.components.*
import id.xms.xtrakernelmanager.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import java.io.BufferedReader
import java.io.InputStreamReader
import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import id.xms.xtrakernelmanager.data.model.SystemInfo
import kotlin.text.isNotBlank


@Composable
fun ShimmerEffect(
    isLoading: Boolean,
    contentLoading: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    if (isLoading) {
        contentLoading()
    } else {
        content()
    }
}

@Composable
fun FadeInEffect(
    shimmerEnabled: Boolean = true,
    content: @Composable (Modifier) -> Unit
) {
    val visibleState = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) {
        visibleState.targetState = true
    }

    val transition = rememberTransition(visibleState, label = "FadeInTransition")
    val alpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 500) },
        label = "alpha"
    ) { if (it) 1f else 0f }

    val shimmerBrush = if (shimmerEnabled) {
        val shimmerColors = listOf(
            Color.LightGray.copy(alpha = 0.6f),
            Color.LightGray.copy(alpha = 0.2f),
            Color.LightGray.copy(alpha = 0.6f),
        )
        val translateAnim = rememberInfiniteTransition(label = "shimmerTransitionFadeIn").animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ), label = "shimmerTranslateFadeIn"
        )
        Brush.linearGradient(
            colors = shimmerColors,
            start = androidx.compose.ui.geometry.Offset.Zero,
            end = androidx.compose.ui.geometry.Offset(x = translateAnim.value, y = translateAnim.value)
        )
    } else null

    Box(modifier = Modifier.alpha(alpha)) {
        content(if (shimmerBrush != null) Modifier.graphicsLayer(alpha = 0.99f)
            else Modifier)
    }
}


private const val TAG_SYSTEM_INFO_UTIL = "SystemInfoUtil"
private const val VALUE_UNKNOWN_SYS_INFO = "Unknown"

fun getSystemInfoFromDevice(): SystemInfo { // Ubah nama fungsi agar lebih jelas
    Log.d(TAG_SYSTEM_INFO_UTIL, "Mengambil SystemInfo (API based)...")
    var socName = VALUE_UNKNOWN_SYS_INFO
    try {
        val manufacturerProcess = Runtime.getRuntime().exec("getprop ro.soc.manufacturer")
        val manufacturer = BufferedReader(InputStreamReader(manufacturerProcess.inputStream)).readLine()?.trim()
        manufacturerProcess.waitFor()
        manufacturerProcess.destroy()

        val modelProcess = Runtime.getRuntime().exec("getprop ro.soc.model")
        val model = BufferedReader(InputStreamReader(modelProcess.inputStream)).readLine()?.trim()
        modelProcess.waitFor()
        modelProcess.destroy()

        if (!manufacturer.isNullOrBlank() && !model.isNullOrBlank()) {
            socName = when {
                manufacturer.equals("QTI", ignoreCase = true) && model.equals("SM7475", ignoreCase = true) -> "Qualcomm® Snapdragon™ 7+ Gen 2"
                manufacturer.equals("QTI", ignoreCase = true) && model.equals("SM8650", ignoreCase = true) -> "Qualcomm® Snapdragon™ 8 Gen 3"
                manufacturer.equals("QTI", ignoreCase = true) && model.equals("SM8635", ignoreCase = true) -> "Qualcomm® Snapdragon™ 8s Gen 3"
                manufacturer.equals("QTI", ignoreCase = true) && (model.equals("SDM845", ignoreCase = true) || model.equals("sdm845", ignoreCase = true)) -> "Qualcomm® Snapdragon™ 845"
                manufacturer.equals("QTI", ignoreCase = true) && model.equals("SM8250", ignoreCase = true) -> "Qualcomm® Snapdragon™ 870"
                manufacturer.equals("QTI", ignoreCase = true) && model.equals("SM8150", ignoreCase = true) -> "Qualcomm® Snapdragon™ 860"
                manufacturer.equals("QTI", ignoreCase = true) && (model.equals("SM7435-AB", ignoreCase = true) || model.equals("SM7435", ignoreCase = true)) -> "Qualcomm® Snapdragon™ 7s Gen 2"
                manufacturer.equals("QTI", ignoreCase = true) && (model.equals("SM8735", ignoreCase = true) || model.equals("sm8735", ignoreCase = true)) -> "Qualcomm® Snapdragon™ 8s Gen 4"
                manufacturer.equals("QTI", ignoreCase = true) && (model.equals("SDM665", ignoreCase = true) || model.equals("sdm665", ignoreCase = true)) -> "Qualcomm® Snapdragon™ 665"
                manufacturer.equals("QTI", ignoreCase = true) && (model.equals("SDM660", ignoreCase = true) || model.equals("sdm660", ignoreCase = true)) -> "Qualcomm® Snapdragon™ 660"
                manufacturer.equals("QTI", ignoreCase = true) && model.equals("SM8750", ignoreCase = true) -> "Qualcomm® Snapdragon™ 8 Elite"
                manufacturer.equals("Mediatek", ignoreCase = true) && (model.equals("MT6785V/CD", ignoreCase = true) || model.equals("MT6785", ignoreCase = true)) -> "MediaTek Helio G95"
                manufacturer.equals("Mediatek", ignoreCase = true) && (model.equals("MT6877V/TTZA", ignoreCase = true) || model.equals("MT6877V", ignoreCase = true)) -> "MediaTek Dimensity 1080"
                manufacturer.equals("Mediatek", ignoreCase = true) && model.equals("MT6833GP", ignoreCase = true) -> "MediaTek Dimensity 6080"
                manufacturer.equals("Mediatek", ignoreCase = true) && model.equals("MT6769Z", ignoreCase = true) -> "MediaTek Helio G85"
                manufacturer.equals("Mediatek", ignoreCase = true) && model.equals("MT6989W", ignoreCase = true) -> "MediaTek Dimensity 9300+"
                else -> "$manufacturer $model"
            }
        }
    } catch (e: Exception) {
        Log.w(TAG_SYSTEM_INFO_UTIL, "Gagal mendapatkan info SOC dari getprop", e)
    }
    return SystemInfo(
        model = android.os.Build.MODEL,
        codename = android.os.Build.DEVICE,
        androidVersion = android.os.Build.VERSION.RELEASE,
        sdk = android.os.Build.VERSION.SDK_INT,
        fingerprint = android.os.Build.FINGERPRINT,
        soc = socName
    ).also { Log.d(TAG_SYSTEM_INFO_UTIL, "SystemInfo: $it") }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: HomeViewModel = hiltViewModel(), navController: NavController) {
    // Kumpulkan semua state dari ViewModel
    val cpuInfo by vm.cpuInfo.collectAsState()
    val batteryInfo by vm.batteryInfo.collectAsState()
    val memoryInfo by vm.memoryInfo.collectAsState()
    val deepSleepInfo by vm.deepSleep.collectAsState()
    val rootStatus by vm.rootStatus.collectAsState()
    val kernelInfo by vm.kernelInfo.collectAsState()
    val appVersion by vm.appVersion.collectAsState()
    val systemInfoState by vm.systemInfo.collectAsState()

    var showFabMenu by remember { mutableStateOf(false) }



    val fullTitle = stringResource(R.string.xtra_kernel_manager)
    val isTitleAnimationDone by vm.isTitleAnimationDone.collectAsState()
    var displayedTitle by remember {
        mutableStateOf(if (isTitleAnimationDone) fullTitle else "")
    }

    LaunchedEffect(isTitleAnimationDone) {
        if (!isTitleAnimationDone) {
            var currentIndex = 0
            while (currentIndex <= fullTitle.length) {
                displayedTitle = fullTitle.substring(0, currentIndex)
                currentIndex++
                delay(100)
            }
            vm.onTitleAnimationFinished()
        }
    }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                AnimatedVisibility(visible = showFabMenu) {
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // FAB Menu Items (Reboot, SystemUI, etc.)
                        SmallFabWithLabel(
                            text = stringResource(R.string.reboot_system),
                            icon = Icons.Filled.Refresh,
                            contentDescription = "Reboot System",
                            onClick = { Runtime.getRuntime().exec(arrayOf("su", "-c", "reboot")) }
                        )
                        SmallFabWithLabel(
                            text = stringResource(R.string.reboot_systemui),
                            icon = Icons.Filled.SettingsApplications,
                            contentDescription = "Reboot SystemUI",
                            onClick = { Runtime.getRuntime().exec(arrayOf("su", "-c", "killall com.android.systemui")) }
                        )
                        SmallFabWithLabel(
                            text = stringResource(R.string.reboot_bootloader),
                            icon = Icons.Filled.Build,
                            contentDescription = "Reboot Bootloader",
                            onClick = { Runtime.getRuntime().exec(arrayOf("su", "-c", "reboot bootloader")) }
                        )
                        SmallFabWithLabel(
                            text = stringResource(R.string.reboot_recovery),
                            icon = Icons.Filled.SettingsBackupRestore,
                            contentDescription = "Reboot Recovery",
                            onClick = { Runtime.getRuntime().exec(arrayOf("su", "-c", "reboot recovery")) }
                        )
                        SmallFabWithLabel(
                            text = stringResource(R.string.power_off),
                            icon = Icons.Filled.PowerSettingsNew,
                            contentDescription = "Power Off",
                            onClick = { Runtime.getRuntime().exec(arrayOf("su", "-c", "reboot -p")) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                FloatingActionButton(
                    onClick = { showFabMenu = !showFabMenu },
                ) {
                    val iconRotation by animateFloatAsState(
                        targetValue = if (showFabMenu) 45f else 0f,
                        animationSpec = tween(durationMillis = 300), label = "fabIconRotation"
                    )
                    Icon(
                        imageVector = Icons.Filled.PowerSettingsNew,
                        contentDescription = "Toggle FAB Menu",
                        modifier = Modifier.graphicsLayer(rotationZ = iconRotation)
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = displayedTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        modifier = Modifier
                            .background(Color(0xFF006400), shape = MaterialTheme.shapes.medium)
                            .clip(MaterialTheme.shapes.large)
                            .padding(horizontal = 8.dp, vertical = 2.dp)


                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            /* 1. CPU */
            FadeInEffect { modifier ->
                val currentSystemInfo = systemInfoState
                val socNameToDisplay = currentSystemInfo?.soc?.takeIf { it.isNotBlank() && it != VALUE_UNKNOWN_SYS_INFO }
                    ?: cpuInfo.soc.takeIf { it.isNotBlank() && it != "Unknown SoC" && it != "N/A" }
                    ?: "CPU"
                CpuCard(socNameToDisplay, cpuInfo, false, modifier)
            }

            /* 2. Merged card */
            val currentBattery = batteryInfo
            val currentMemory = memoryInfo
            val currentDeepSleep = deepSleepInfo
            val currentRoot = rootStatus
            val currentVersion = appVersion
            val currentSystem = systemInfoState

            if (currentBattery != null && currentMemory != null && currentDeepSleep != null &&
                currentRoot != null && currentVersion != null && currentSystem != null) {
                FadeInEffect { modifier ->
                    MergedSystemCard(
                        b = currentBattery,
                        d = currentDeepSleep,
                        rooted = currentRoot,
                        version = currentVersion,
                        blur = false,
                        mem = currentMemory,
                        systemInfo = currentSystem,
                        modifier = modifier
                    )
                }
            } else {
                FadeInEffect { modifier ->
                     Box(modifier.fillMaxWidth().height(200.dp).background(Color.LightGray.copy(alpha = 0.5f))) {
                         CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                     }
                }
            }


            /* 3. Kernel */
            val currentKernel = kernelInfo
            if (currentKernel != null) {
                FadeInEffect { modifier ->
                    KernelCard(currentKernel, false, modifier)
                }
            } else {
                // Opsional: Placeholder untuk KernelCard
            }


            /* 4. About */
            FadeInEffect { modifier ->
                AboutCard(false, modifier)
            }
        }
    }
}


@Composable
private fun SmallFabWithLabel(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
            )
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                textAlign = TextAlign.Center
            )
        }
        SmallFloatingActionButton(onClick = onClick) {
            Icon(icon, contentDescription)
        }
    }
}
