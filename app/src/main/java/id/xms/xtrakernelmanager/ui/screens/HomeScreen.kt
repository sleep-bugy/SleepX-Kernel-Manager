package id.xms.xtrakernelmanager.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import id.xms.xtrakernelmanager.R
import id.xms.xtrakernelmanager.ui.components.*
import id.xms.xtrakernelmanager.viewmodel.HomeViewModel
import kotlinx.coroutines.delay

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
    visibleState.targetState = true // Trigger the animation

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
        val translateAnim = rememberInfiniteTransition(label = "shimmerTransition").animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ), label = "shimmerTranslate"
        )
        Brush.linearGradient(
            colors = shimmerColors,
            start = androidx.compose.ui.geometry.Offset.Zero,
            end = androidx.compose.ui.geometry.Offset(x = translateAnim.value, y = translateAnim.value)
        )
    } else null

    Box(modifier = Modifier.alpha(alpha)) {
        content(if (shimmerBrush != null) Modifier.graphicsLayer(alpha = 0.99f) else Modifier)
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: HomeViewModel = hiltViewModel()) {
    val cpuInfo by vm.cpuInfo.collectAsState()
    val battery = vm.batteryInfo
    val memory = vm.memoryInfo
    val deepSleep by vm.deepSleep.collectAsState()
    val root = vm.rootStatus
    val kernel = vm.kernelInfo
    val version = vm.appVersion
    var blurOn by rememberSaveable { mutableStateOf(true) }

    // Shimmer animation for TopAppBar
    val shimmerColors = listOf(
        Color.White.copy(alpha = 0.0f),
        Color.White.copy(alpha = 0.5f),
        Color.White.copy(alpha = 0.0f)
    )
    val infiniteTransition = rememberInfiniteTransition(label = "topAppBarShimmer")
    val translateAnim by infiniteTransition.animateFloat(
        initialValue = -100f, // Start off-screen to the left
        targetValue = 1000f, // End off-screen to the right (adjust based on title width)
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "topAppBarShimmerTranslate"
    )
    val shimmerBrush = Brush.linearGradient(
        colors = shimmerColors,
    )
    val fullTitle = stringResource(R.string.xtra_kernel_manager)
    var displayedTitle by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        var currentIndex = 0
        while (currentIndex <= fullTitle.length) {
            displayedTitle = fullTitle.substring(0, currentIndex)
            currentIndex++
            delay(100) // Adjust delay for typing speed
        }
        // Optional: Add a blinking cursor effect after typing
        while (true) {
            delay(500)
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp) // Standard TopAppBar height
            ) {
                TopAppBar(
                    title = {
                        Box {
                            Text(
                                text = displayedTitle,
                                style = MaterialTheme.typography.headlineLarge
                            )
                            // Apply shimmer effect over the text
                            androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                                drawIntoCanvas {
                                    drawRect(
                                        brush = shimmerBrush,
                                        topLeft = androidx.compose.ui.geometry.Offset(translateAnim, 0f),
                                        size = androidx.compose.ui.geometry.Size(100f, size.height) // Adjust width of shimmer
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (blurOn) Color.Transparent else MaterialTheme.colorScheme.surface
                    ),
                    actions = {
                        IconToggleButton(
                            checked = blurOn,
                            onCheckedChange = { blurOn = it },
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Icon(
                                imageVector = if (blurOn) Icons.Default.BlurOn else Icons.Default.BlurOff,
                                contentDescription = "Toggle blur"
                            )
                        }
                    }
                )
            }
        }
    ) { padding ->

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            /* 1. CPU */
            FadeInEffect { modifier -> CpuCard(cpuInfo, blurOn, modifier) }

            /* 2. Merged card */
            FadeInEffect { modifier -> MergedSystemCard(battery, deepSleep, root, version, blurOn, memory, modifier) }

            /* 3. Kernel */
            FadeInEffect { modifier -> KernelCard(kernel, blurOn, modifier) }

            /* 4. About */
            FadeInEffect { modifier -> AboutCard(blurOn, modifier) }
        }
    }
}
