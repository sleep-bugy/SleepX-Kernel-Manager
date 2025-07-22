package id.xms.xtrakernelmanager.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CpuClusterCard(
    coreCount: Int,
    maxFreq: String,
    minFreq: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "CPU",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp)
            )
            Spacer(modifier = Modifier.padding(4.dp))
            Row { Text(text = "Cores: $coreCount", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)) }
            Row { Text(text = "Max Freq: $maxFreq MHz", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)) }
            Row { Text(text = "Min Freq: $minFreq MHz", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)) }
        }
    }
}