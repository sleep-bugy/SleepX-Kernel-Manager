package id.xms.xtrakernelmanager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun TerminalShortcutCard(onClick: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
        listOf("dmesg", "logcat", "reboot").forEach {
            Button(onClick = { onClick(it) }) { Text(it) }
        }
    }
}