package id.xms.xtrakernelmanager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SwappinessCard(
    value: Int,
    onValueChange: (Int) -> Unit,
    blur: Boolean
) {
    GlassCard(blur) {
        Column(Modifier.padding(16.dp)) {
            Text("Swappiness", style = MaterialTheme.typography.titleMedium)
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = 0f..100f
            )
            Text("Current: $value")
        }
    }
}