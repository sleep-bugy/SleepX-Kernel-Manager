package id.xms.xtrakernelmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.xms.xtrakernelmanager.R
import id.xms.xtrakernelmanager.data.model.RealtimeCpuInfo
import kotlinx.coroutines.delay
import kotlin.random.Random

enum class GraphMode {
    SPEED,
    LOAD
}

const val MAX_HISTORY_POINTS_GRAPH = 40
const val SIMULATE_CPU_LOAD_TOGGLE = false

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CpuCard(
    info: RealtimeCpuInfo,
    blur: Boolean,
    modifier: Modifier = Modifier
) {
    var graphDataHistory by remember { mutableStateOf(listOf<Float>()) }
    var currentGraphMode by remember { mutableStateOf(GraphMode.LOAD) }

    LaunchedEffect(currentGraphMode, info) {
        val currentDataPoint: Float = when (currentGraphMode) {
            GraphMode.SPEED -> {
                if (info.freqs.isNotEmpty()) {
                    info.freqs.map { it.toFloat() }.average().toFloat()
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
                    (info.cpuLoadPercentage ?: (Random.nextFloat() * 100f)).coerceIn(0f,100f)
                }
            }
        }
        graphDataHistory = (graphDataHistory + currentDataPoint).takeLast(MAX_HISTORY_POINTS_GRAPH)
    }

    GlassCard(blur, modifier) {
        val infiniteTransition = rememberInfiniteTransition(label = "shimmer_transition_cpu_card_font")
        val shimmerAlpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ), label = "shimmer_alpha_cpu_card_font"
        )
        val graphColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = shimmerAlpha)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // --- Bagian Info CPU dan Frekuensi Inti ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            stringResource(R.string.central_proccessing_unit_cpu),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("CPU", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    if (info.freqs.isNotEmpty()) {
                        info.freqs.chunked(4).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically // Pusatkan vertikal
                            ) {
                                pair.forEachIndexed { index, freq ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(2.dp) // Sedikit padding antar box
                                            .clip(MaterialTheme.shapes.small)
                                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
                                            .padding(vertical = 4.dp, horizontal = 6.dp), // Padding dalam box
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$freq MHz",
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontSize = 22.sp // Sesuaikan ukuran font jika perlu agar muat
                                            ),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }

                                }
                                if (pair.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Info Tambahan (Governor, Cores, Temp)
                    val infoTextStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 20.sp
                    )
                    val infoTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)

                    Text(
                        stringResource(R.string.cpu_governor, info.governor),
                        style = infoTextStyle,
                        color = infoTextColor,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Text(
                        stringResource(R.string.cpu_cores_core, info.cores),
                        style = infoTextStyle,
                        color = infoTextColor,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Text(
                        stringResource(R.string.temperature_c, "%.1f".format(info.temp)),
                        style = infoTextStyle,
                        color = infoTextColor
                    )


                    if (currentGraphMode == GraphMode.LOAD && graphDataHistory.isNotEmpty()) {
                        Text(
                            "Avg. Load: %.1f%%".format(graphDataHistory.lastOrNull() ?: 0f),
                            style = infoTextStyle.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }

                // --- Grafik Canvas ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (graphDataHistory.size > 1) {
                            val path = Path()
                            val (yAxisMin, yAxisMax) = when (currentGraphMode) {
                                GraphMode.SPEED -> {
                                    val dataMin = graphDataHistory.minOrNull() ?: 0f
                                    val dataMax = (graphDataHistory.maxOrNull() ?: 4000f).coerceAtLeast(dataMin + 100f) // Pastikan dataMax selalu lebih besar dari dataMin
                                    val yPadding = (dataMax - dataMin).coerceAtLeast(100f) * 0.1f
                                    (dataMin - yPadding).coerceAtLeast(0f) to (dataMax + yPadding).coerceAtLeast(dataMin + 100f)
                                }
                                GraphMode.LOAD -> 0f to 100f
                            }
                            val effectiveYRange = (yAxisMax - yAxisMin).coerceAtLeast(1f)
                            val stepX = size.width / (MAX_HISTORY_POINTS_GRAPH - 1).coerceAtLeast(1).toFloat()

                            graphDataHistory.forEachIndexed { index, dataPoint ->
                                val x = size.width - (graphDataHistory.size - 1 - index) * stepX
                                val normalizedData = ((dataPoint - yAxisMin) / effectiveYRange).coerceIn(0f, 1f)
                                val y = size.height * (1 - normalizedData)
                                if (index == 0) path.moveTo(x, y.coerceIn(0f, size.height))
                                else path.lineTo(x, y.coerceIn(0f, size.height))
                            }
                            drawPath(path, graphColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = if (currentGraphMode == GraphMode.SPEED) "Clock Speed" else "CPU Load",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    val switchIcon: ImageVector = if (currentGraphMode == GraphMode.SPEED) Icons.Filled.Speed else Icons.Filled.BarChart
                    Switch(
                        checked = currentGraphMode == GraphMode.LOAD,
                        onCheckedChange = { isChecked ->
                            currentGraphMode = if (isChecked) GraphMode.LOAD else GraphMode.SPEED
                            graphDataHistory = emptyList()
                        },
                        thumbContent = {
                            Icon(switchIcon, "Toggle Graph Mode", Modifier.size(SwitchDefaults.IconSize))
                        }
                    )
                }
            }
        }
    }
}
