package id.xms.xtrakernelmanager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ThermalCard(onModeChange: (String) -> Unit) {
    var selected by remember { mutableStateOf("dynamic") }
    val modes = listOf("dynamic", "incalls", "pubg")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Thermal Mode", style = MaterialTheme.typography.titleMedium)
            modes.forEach { mode ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selected == mode,
                        onClick = { selected = mode; onModeChange(mode) }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(mode)
                }
            }
        }
    }
}