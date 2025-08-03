package id.xms.xtrakernelmanager.ui.components

import android.graphics.BlurMaskFilter.Blur
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.xms.xtrakernelmanager.data.model.KernelInfo

@Composable
fun InfoCard(
    kernel: KernelInfo,
    blur: Boolean,
    modifier: Modifier = Modifier,
) {
    GlassCard(blur, modifier) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text("Kernel", style = MaterialTheme.typography.titleMedium)
                Text("Version: ${kernel.version}")
                Text("GKI: ${kernel.gkiType}")
                Text("Scheduler: ${kernel.scheduler}")
            }
        }

    }
}