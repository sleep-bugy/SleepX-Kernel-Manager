package id.xms.xtrakernelmanager.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.xms.xtrakernelmanager.R
import id.xms.xtrakernelmanager.data.model.RealtimeCpuInfo
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

enum class GraphMode {
    SPEED,
    LOAD
}

const val MAX_HISTORY_POINTS_GRAPH = 50
const val SIMULATE_CPU_LOAD_TOGGLE = false

@Composable
fun CpuCard(
    soc: String,
    info: RealtimeCpuInfo,
    blur: Boolean,
    modifier: Modifier = Modifier
) {
    var graphDataHistory by remember { mutableStateOf(listOf<Float>()) }
    var currentGraphMode by remember { mutableStateOf(GraphMode.LOAD) }

    // Animation untuk pulse effect
    val infiniteTransition = rememberInfiniteTransition(label = "cpu_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    LaunchedEffect(currentGraphMode, info) {
        val currentDataPoint: Float = when (currentGraphMode) {
            GraphMode.SPEED -> {
                if (info.freqs.isNotEmpty()) {
                    info.freqs.filter { it > 0 }.map { it.toFloat() }.average().toFloat().takeIf { !it.isNaN() } ?: 0f
                } else {
                    0f
                }
            }
            GraphMode.LOAD -> {
                if (SIMULATE_CPU_LOAD_TOGGLE) {
                    delay(150)
                    val baseLoad = Random.nextFloat() * 70f
                    val spike = if (Random.nextInt(0, 4) == 0) Random.nextFloat() * 30f else 0f
                    (baseLoad + spike).coerceIn(0f, 100f)
                } else {
                    (info.cpuLoadPercentage ?: (Random.nextFloat() * 100f)).coerceIn(0f, 100f)
                }
            }
        }
        graphDataHistory = (graphDataHistory + currentDataPoint).takeLast(MAX_HISTORY_POINTS_GRAPH)
    }

    // Enhanced Glass Card dengan intensitas yang lebih ringan agar konten terlihat
    SuperGlassCard(
        modifier = modifier,
        glassIntensity = GlassIntensity.Light, // Ubah dari Heavy ke Light
        onClick = null
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header dengan SoC info dan animated icon
            CpuHeaderSection(soc = soc, info = info, pulseAlpha = pulseAlpha)

            // CPU cores frequency dengan glassmorphism
            if (info.freqs.isNotEmpty()) {
                CpuCoresSection(info = info)
            }

            // Stats dengan icons dan colors
            CpuStatsSection(info = info, currentGraphMode = currentGraphMode, graphDataHistory = graphDataHistory)

            // Enhanced graph dengan gradient dan glow
            EnhancedCpuGraph(
                graphDataHistory = graphDataHistory,
                currentGraphMode = currentGraphMode,
                pulseAlpha = pulseAlpha
            )

            // Modern toggle switch
            CpuGraphModeToggle(
                currentGraphMode = currentGraphMode,
                onModeChanged = { newMode ->
                    currentGraphMode = newMode
                    graphDataHistory = emptyList()
                }
            )
        }
    }
}

@Composable
private fun CpuHeaderSection(
    soc: String,
    info: RealtimeCpuInfo,
    pulseAlpha: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = soc.takeIf { it.isNotBlank() && it != "Unknown SoC" && it != "N/A" }
                    ?: info.soc.takeIf { it.isNotBlank() && it != "Unknown SoC" && it != "N/A" }
                    ?: stringResource(R.string.central_proccessing_unit_cpu),
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
                    text = if ((soc.isNotBlank() && soc != "Unknown SoC" && soc != "N/A") ||
                        (info.soc.isNotBlank() && info.soc != "Unknown SoC" && info.soc != "N/A"))
                        stringResource(R.string.cpu_soc_label) else stringResource(R.string.cpu_cpu_label),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                )
            }
        }

        // Animated CPU icon with pulse effect
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .drawBehind {
                    // Pulsing glow effect
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF6750A4).copy(alpha = pulseAlpha * 0.6f),
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
                imageVector = Icons.Default.Memory,
                contentDescription = "CPU",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun CpuCoresSection(info: RealtimeCpuInfo) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "CPU Cores",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
        )

        val activeCoresFreq = info.freqs
        activeCoresFreq.chunked(4).forEach { freqsInRow ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                freqsInRow.forEach { freq ->
                    GlassmorphismSurface(
                        modifier = Modifier.weight(1f),
                        blurRadius = 6f,
                        alpha = 0.25f
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp, horizontal = 8.dp)
                        ) {
                            if (freq == 0) {
                                Text(
                                    text = "OFF",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Text(
                                    text = "${freq}",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "MHz",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // Fill remaining space if needed
                repeat((4 - freqsInRow.size).coerceAtLeast(0)) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CpuStatsSection(
    info: RealtimeCpuInfo,
    currentGraphMode: GraphMode,
    graphDataHistory: List<Float>
) {
    // Gabungkan semua stats dalam 1 card dengan glassmorphism yang matching
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "System Stats",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Governor
                    StatItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Speed,
                        label = "Governor",
                        value = info.governor.takeIf { it.isNotBlank() } ?: "N/A",
                        color = MaterialTheme.colorScheme.secondary
                    )

                    // Temperature
                    StatItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Thermostat,
                        label = "Temperature",
                        value = "${info.temp}°C",
                        color = when {
                            info.temp > 80 -> MaterialTheme.colorScheme.error
                            info.temp > 60 -> Color(0xFFFF9800)
                            else -> MaterialTheme.colorScheme.tertiary
                        }
                    )

                    // Load/Cores
                    if (currentGraphMode == GraphMode.LOAD && graphDataHistory.isNotEmpty()) {
                        StatItem(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.BarChart,
                            label = "Load",
                            value = "${graphDataHistory.lastOrNull()?.toInt() ?: 0}%",
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        StatItem(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Memory,
                            label = "Cores",
                            value = "${info.cores}",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(20.dp)
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = color,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun EnhancedCpuGraph(
    graphDataHistory: List<Float>,
    currentGraphMode: GraphMode,
    pulseAlpha: Float
) {
    GlassmorphismSurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        blurRadius = 0f,
        alpha = 0.4f
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            if (graphDataHistory.size > 1) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val (yAxisMin, yAxisMax) = when (currentGraphMode) {
                        GraphMode.SPEED -> {
                            val dataMin = graphDataHistory.filter { it > 0 }.minOrNull() ?: 0f
                            val dataMax = (graphDataHistory.filter { it > 0 }.maxOrNull() ?: 4000f).coerceAtLeast(dataMin + 100f)
                            val yPadding = (dataMax - dataMin).coerceAtLeast(100f) * 0.1f
                            (dataMin - yPadding).coerceAtLeast(0f) to (dataMax + yPadding).coerceAtLeast(dataMin + 100f)
                        }
                        GraphMode.LOAD -> 0f to 100f
                    }

                    val effectiveYRange = (yAxisMax - yAxisMin).coerceAtLeast(1f)
                    val stepX = size.width / (MAX_HISTORY_POINTS_GRAPH - 1).coerceAtLeast(1).toFloat()

                    // Create gradient path for fill
                    val path = Path()
                    val fillPath = Path()

                    graphDataHistory.forEachIndexed { index, dataPoint ->
                        val x = size.width - (graphDataHistory.size - 1 - index) * stepX
                        val normalizedData = ((dataPoint.coerceAtLeast(yAxisMin) - yAxisMin) / effectiveYRange).coerceIn(0f, 1f)
                        val y = size.height * (1 - normalizedData)

                        if (index == 0) {
                            path.moveTo(x, y.coerceIn(0f, size.height))
                            fillPath.moveTo(x, size.height)
                            fillPath.lineTo(x, y.coerceIn(0f, size.height))
                        } else {
                            path.lineTo(x, y.coerceIn(0f, size.height))
                            fillPath.lineTo(x, y.coerceIn(0f, size.height))
                        }
                    }

                    // Close fill path
                    fillPath.lineTo(size.width, size.height)
                    fillPath.close()

                    // Draw gradient fill
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF6750A4).copy(alpha = pulseAlpha * 0.3f),
                                Color(0xFF6750A4).copy(alpha = 0.05f)
                            )
                        )
                    )

                    // Draw main line with glow effect
                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF6750A4).copy(alpha = 0.6f),
                                Color(0xFF7C4DFF).copy(alpha = 0.8f),
                                Color(0xFF6750A4).copy(alpha = 0.6f)
                            )
                        ),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // Draw glow effect
                    drawPath(
                        path = path,
                        color = Color(0xFF6750A4).copy(alpha = pulseAlpha * 0.4f),
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // Draw data points
                    graphDataHistory.forEachIndexed { index, dataPoint ->
                        val x = size.width - (graphDataHistory.size - 1 - index) * stepX
                        val normalizedData = ((dataPoint.coerceAtLeast(yAxisMin) - yAxisMin) / effectiveYRange).coerceIn(0f, 1f)
                        val y = size.height * (1 - normalizedData)

                        if (index == graphDataHistory.size - 1) { // Latest point
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.9f),
                                        Color(0xFF6750A4).copy(alpha = 0.7f)
                                    )
                            ),
                            radius = 6.dp.toPx(),
                            center = Offset(x, y.coerceIn(0f, size.height))
                        )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Gathering data...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CpuGraphModeToggle(
    currentGraphMode: GraphMode,
    onModeChanged: (GraphMode) -> Unit
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (currentGraphMode == GraphMode.SPEED) Icons.Default.Speed else Icons.Default.BarChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (currentGraphMode == GraphMode.SPEED)
                            "Clock Speed" else "CPU Load",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Toggle switch tanpa blur
                Switch(
                    checked = currentGraphMode == GraphMode.LOAD,
                    onCheckedChange = { isChecked ->
                        onModeChanged(if (isChecked) GraphMode.LOAD else GraphMode.SPEED)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    thumbContent = {
                        Icon(
                            imageVector = if (currentGraphMode == GraphMode.SPEED) Icons.Default.Speed else Icons.Default.BarChart,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                            tint = Color.White
                        )
                    }
                )
            }
        }
    }
}
