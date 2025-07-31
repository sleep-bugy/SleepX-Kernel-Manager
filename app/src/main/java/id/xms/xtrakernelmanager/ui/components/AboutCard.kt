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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import id.xms.xtrakernelmanager.R

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
            Text(stringResource(id = R.string.about), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(id = R.string.desc_about))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(onClick = { /* open Telegram */ }) {
                    Icon(Icons.AutoMirrored.Filled.Send, stringResource(id = R.string.telegram))
                }
                IconButton(onClick = { /* open GitHub */ }) {
                    Icon(Icons.Default.Code, stringResource(id = R.string.github))
                }
            }
        }
    }
}