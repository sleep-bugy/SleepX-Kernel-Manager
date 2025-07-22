package id.xms.xtrakernelmanager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.xms.xtrakernelmanager.util.RootUtils

@Composable
fun HomeScreen(
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier
) {
    var isRooted by remember { mutableStateOf(false) }
    isRooted = RootUtils.isDeviceRooted() // Cek status root saat composable di-render

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Root Status",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Device is ${if (isRooted) "Rooted" else "Not Rooted"}",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isRooted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
    }
}