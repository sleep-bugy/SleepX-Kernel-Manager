package id.xms.xtrakernelmanager.ui.components

import android.graphics.BlurMaskFilter.Blur
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.xms.xtrakernelmanager.data.model.SystemInfo

@Composable
fun SystemCard(
    info: SystemInfo,
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, contentDescription = "System Info")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("System", style = MaterialTheme.typography.titleMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Build, contentDescription = "Model")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Model: ${info.model} (${info.codename})")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Android, contentDescription = "Android Version")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Android ${info.androidVersion} (SDK ${info.sdk})")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Build, contentDescription = "Build Fingerprint")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Build: ${info.fingerprint}")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // TODO: Replace with a more appropriate icon for SOC
                    //  For now, using Icons.Outlined.Info as a placeholder.
                    //  Consider using a custom icon or a more generic hardware icon.
                    Icon(androidx.compose.material.icons.Icons.Outlined.Info, contentDescription = "SOC")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SOC: ${info.soc}")
                }
            }
        }
    }
}