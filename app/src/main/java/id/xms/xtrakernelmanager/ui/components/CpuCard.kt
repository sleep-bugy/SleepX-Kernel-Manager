package id.xms.xtrakernelmanager.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.xms.xtrakernelmanager.R
import id.xms.xtrakernelmanager.data.model.RealtimeCpuInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CpuCard(
    info: RealtimeCpuInfo,
    blur: Boolean,
    modifier: Modifier = Modifier
) {
    GlassCard(blur, modifier) {
        val infiniteTransition = rememberInfiniteTransition(label = "shimmer_transition")
        val shimmerAlpha = infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ), label = "shimmer_alpha"
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = shimmerAlpha.value)
            )
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(stringResource(R.string.central_proccessing_unit_cpu), style = MaterialTheme.typography.titleLarge)
                Text("${info.freqs.joinToString { "$it MHz" }}")
                Text(stringResource(R.string.cpu_cores_core, info.cores))
                Text(stringResource(R.string.cpu_governor, info.governor))
                Text(stringResource(R.string.temperature_c, "%.1f".format(info.temp)))
            }
        }
    }
}