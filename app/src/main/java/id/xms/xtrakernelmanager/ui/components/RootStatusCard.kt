package id.xms.xtrakernelmanager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
fun RootStatusCard(
    isRooted: Boolean,
    showEasterEgg: Boolean,
    onClick: () -> Unit,
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
                text = "Root Status",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                modifier = Modifier.clickable(onClick = onClick)
            )
            Spacer(modifier = Modifier.padding(4.dp))
            Text(
                text = "Device is ${if (isRooted) "Rooted" else "Not Rooted"}",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = if (isRooted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            if (showEasterEgg) {
                Text(
                    text = "Xtra Power! 🎉",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}