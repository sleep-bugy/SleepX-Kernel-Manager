package id.xms.xtrakernelmanager.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AboutCard(
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
            Text("About", style = MaterialTheme.typography.titleMedium)
            Text("Open-source kernel manager for rooted devices, I Just Want To Say KONTOL")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(onClick = { /* open Telegram */ }) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Telegram")
                }
                IconButton(onClick = { /* open GitHub */ }) {
                    Icon(Icons.Default.Code, "GitHub")
                }
            }
        }
    }
}