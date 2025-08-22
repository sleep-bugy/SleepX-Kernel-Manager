package id.xms.xtrakernelmanager.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import id.xms.xtrakernelmanager.data.model.RealtimeCpuInfo // Pastikan ini adalah path yang benar
import kotlinx.coroutines.delay
import kotlin.random.Random

enum class GraphMode {
    SPEED,
    LOAD
}

const val MAX_HISTORY_POINTS_GRAPH = 40
const val SIMULATE_CPU_LOAD_TOGGLE = false // Anda mungkin ingin mengontrol ini dari ViewModel atau tempat lain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CpuCard(
    soc: String,
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
                    info.freqs.filter { it > 0 }.map { it.toFloat() }.average().toFloat().takeIf { !it.isNaN() } ?: 0f
                } else {
                    0f
                }
            }
            GraphMode.LOAD -> {
                if (SIMULATE_CPU_LOAD_TOGGLE) {
                    delay(150) // Hanya untuk simulasi, idealnya data load dari sumber nyata
                    val baseLoad = Random.nextFloat() * 70f
                    val spike = if (Random.nextInt(0, 4) == 0) Random.nextFloat() * 30f else 0f
                    (baseLoad + spike).coerceIn(0f, 100f)
                } else {
                    // Gunakan cpuLoadPercentage jika tersedia, fallback ke nilai acak hanya jika null (seperti sebelumnya)
                    (info.cpuLoadPercentage ?: (Random.nextFloat() * 100f)).coerceIn(0f, 100f)
                }
            }
        }
        graphDataHistory = (graphDataHistory + currentDataPoint).takeLast(MAX_HISTORY_POINTS_GRAPH)
    }

    GlassCard(blur, modifier) {
        val graphColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // --- Bagian Info SoC dan Frekuensi Inti ---
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
                            text = soc.takeIf { it.isNotBlank() && it != "Unknown SoC" && it != "N/A"}
                                ?: info.soc.takeIf { it.isNotBlank() && it != "Unknown SoC" && it != "N/A" } ?: stringResource(R.string.central_proccessing_unit_cpu),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
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
                            Text(
                                if ((soc.isNotBlank() && soc != "Unknown SoC" && soc != "N/A") ||
                                    (info.soc.isNotBlank() && info.soc != "Unknown SoC" && info.soc != "N/A")) stringResource(R.string.cpu_soc_label)
                                else stringResource(R.string.cpu_cpu_label),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    if (info.freqs.isNotEmpty()) {
                        // Menampilkan maksimal 2 baris frekuensi (misal 8 core dalam 2 baris)
                        // atau 1 baris jika kurang dari atau sama dengan 4 core aktif.
                        // Anda bisa menyesuaikan logika chunked ini jika arsitektur CPU berbeda (misal 3 cluster)
                        val activeCoresFreq = info.freqs //.filter { it > 0 } // Filter core offline jika ingin
                        activeCoresFreq.chunked(4).forEach { freqsInRow ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween, // Atau Arrangement.spacedBy(4.dp)
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                freqsInRow.forEach { freq ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f) // Agar semua box punya lebar sama
                                            .padding(horizontal = 2.dp, vertical = 2.dp)
                                            .clip(MaterialTheme.shapes.extraSmall) // Bentuk lebih kecil
                                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
                                            .padding(vertical = 4.dp, horizontal = 4.dp), // Padding dalam sedikit dikurangi
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (freq == 0) { // Core offline
                                            Text(
                                                text = stringResource(R.string.offline),
                                                style = MaterialTheme.typography.bodyMedium.copy( // Ukuran font lebih kecil
                                                    fontSize = 14.sp, // Disesuaikan
                                                    fontWeight = FontWeight.Normal
                                                ),
                                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                                            )
                                        } else {
                                            Text(
                                                text = stringResource(R.string.cpu_freq_mhz, freq),
                                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp), // Sedikit lebih kecil
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }
                                // Jika dalam satu baris hanya ada kurang dari 4 item, tambahkan Spacer untuk mengisi sisa ruang
                                repeat((4 - freqsInRow.size).coerceAtLeast(0)) {
                                    Spacer(Modifier.weight(1f).padding(horizontal = 2.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp)) // Jarak antar baris frekuensi
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }


                    // (Governor, Cores, Temp)
                    val infoTextStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp // Sedikit perkecil ukuran font info
                    )
                    val infoValueTextStyle = infoTextStyle.copy(fontWeight = FontWeight.SemiBold)
                    val infoTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            stringResource(R.string.cpu_governor_label), // "Governor:"
                            style = infoTextStyle,
                            color = infoTextColor,
                        )
                        Text(
                            info.governor.takeIf { it.isNotBlank() } ?: stringResource(R.string.common_na),
                            style = infoValueTextStyle,
                            color = infoTextColor,
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            stringResource(R.string.cpu_cores_label), // "Cores:"
                            style = infoTextStyle,
                            color = infoTextColor,
                        )
                        Text(
                            stringResource(R.string.cpu_cores_format, info.cores),
                            style = infoValueTextStyle,
                            color = infoTextColor,
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            stringResource(R.string.temperature_label), // "Temp:"
                            style = infoTextStyle,
                            color = infoTextColor,
                        )
                        Text(
                            stringResource(R.string.temperature_c_format, info.temp), // "%.1f °C"
                            style = infoValueTextStyle,
                            color = infoTextColor
                        )
                    }


                    if (currentGraphMode == GraphMode.LOAD && graphDataHistory.isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                stringResource(R.string.avg_load_label), // "Avg. Load:"
                                style = infoTextStyle,
                                color = infoTextColor,
                            )
                            Text(
                                stringResource(R.string.cpu_avg_load_format, graphDataHistory.lastOrNull() ?: 0f),
                                style = infoValueTextStyle.copy(color = MaterialTheme.colorScheme.primary), // Warna beda untuk load
                                // modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }

                // --- Grafik Canvas ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp) // Ketinggian grafik
                        .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 8.dp) // Kurangi padding atas
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (graphDataHistory.size > 1) {
                            val path = Path()
                            val (yAxisMin, yAxisMax) = when (currentGraphMode) {
                                GraphMode.SPEED -> {
                                    val dataMin = graphDataHistory.filter{ it > 0 }.minOrNull() ?: 0f
                                    val dataMax = (graphDataHistory.filter{ it > 0 }.maxOrNull() ?: info.freqs.maxOrNull()?.toFloat() ?: 4000f).coerceAtLeast(dataMin + 100f)
                                    val yPadding = (dataMax - dataMin).coerceAtLeast(100f) * 0.1f // 10% padding
                                    (dataMin - yPadding).coerceAtLeast(0f) to (dataMax + yPadding).coerceAtLeast(dataMin + 100f)
                                }
                                GraphMode.LOAD -> 0f to 100f // Skala 0-100% untuk load
                            }
                            val effectiveYRange = (yAxisMax - yAxisMin).coerceAtLeast(1f) // Hindari pembagian dengan nol
                            val stepX = size.width / (MAX_HISTORY_POINTS_GRAPH - 1).coerceAtLeast(1).toFloat()

                            graphDataHistory.forEachIndexed { index, dataPoint ->
                                val x = size.width - (graphDataHistory.size - 1 - index) * stepX
                                // Normalisasi data point, pastikan tidak negatif jika yAxisMin > dataPoint (meski seharusnya tidak terjadi dgn coerceAtLeast(0f))
                                val normalizedData = ((dataPoint.coerceAtLeast(yAxisMin) - yAxisMin) / effectiveYRange).coerceIn(0f, 1f)
                                val y = size.height * (1 - normalizedData) // Invert Y karena canvas 0,0 di kiri atas

                                if (index == 0) {
                                    path.moveTo(x, y.coerceIn(0f, size.height))
                                } else {
                                    path.lineTo(x, y.coerceIn(0f, size.height))
                                }
                            }
                            drawPath(
                                path = path,
                                color = graphColor,
                                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }
                }
                // --- Tombol Switch Mode Grafik ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 0.dp), // Kurangi padding vertikal
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween // Pindahkan ke kanan
                ) {
                    Text(
                        text = if (currentGraphMode == GraphMode.SPEED) stringResource(R.string.clock_speed) else stringResource(R.string.cpu_load),
                        style = MaterialTheme.typography.labelMedium, // Label lebih kecil
                    )
                    val switchIcon: ImageVector = if (currentGraphMode == GraphMode.SPEED) Icons.Filled.Speed else Icons.Filled.BarChart
                    Switch(
                        checked = currentGraphMode == GraphMode.LOAD,
                        onCheckedChange = { isChecked ->
                            currentGraphMode = if (isChecked) GraphMode.LOAD else GraphMode.SPEED
                            graphDataHistory = emptyList() // Reset history saat ganti mode
                        },
                        thumbContent = {
                            Icon(switchIcon, stringResource(R.string.cpu_toggle_graph_mode_desc), Modifier.size(SwitchDefaults.IconSize))
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp)) // Beri jarak di bawah switch
            }
        }
    }
}
