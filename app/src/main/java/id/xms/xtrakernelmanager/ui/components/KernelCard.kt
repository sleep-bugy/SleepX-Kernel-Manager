package id.xms.xtrakernelmanager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.xms.xtrakernelmanager.data.model.KernelInfo

@Composable
fun KernelCard(
    k: KernelInfo,
    blur: Boolean,
    modifier: Modifier = Modifier
) {
    GlassCard(blur, modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Kernel", style = MaterialTheme.typography.titleMedium)
            Text("Version: ${k.version.take(30)}…")
            Text("Type: ${k.gkiType}")
        }
    }
}