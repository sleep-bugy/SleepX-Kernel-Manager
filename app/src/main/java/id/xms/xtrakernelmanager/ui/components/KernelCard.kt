package id.xms.xtrakernelmanager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import id.xms.xtrakernelmanager.R
import id.xms.xtrakernelmanager.data.model.KernelInfo

@Composable
fun KernelCard(
    k: KernelInfo,
    blur: Boolean,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.kernel_information), style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.kernel_version, k.version), textAlign = TextAlign.Center)
                    Text(stringResource(R.string.kernel_type, k.gkiType), textAlign = TextAlign.Center)
                    Text(stringResource(R.string.sched, k.scheduler))
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showDialog = false }) {
                        Text("Close")
                    }
                }
            }
        }
    }

    GlassCard(blur, modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f), // Allow Column to take available space
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.kernel), style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.width(8.dp)) // Space between text and icon
                    IconButton(onClick = { showDialog = true }, modifier = Modifier.size(32.dp)) { // Use 32.dp for kernel icon size
                        Icon(
                            painter = painterResource(id = R.drawable.kernel), // Changed to kernel icon
                            contentDescription = stringResource(R.string.kernel_information),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp)) // Added vertical space
                val versionString = k.version
                val hashIndex = versionString.indexOf(" #")
                val parenIndex = versionString.indexOf(" (")

                val endIndex = when {
                    hashIndex != -1 && parenIndex != -1 -> minOf(hashIndex, parenIndex)
                    hashIndex != -1 -> hashIndex
                    parenIndex != -1 -> parenIndex
                    else -> versionString.length // No special characters found, take full string
                }
                val localVersion = versionString.substring(0, endIndex).trim()
                Text(stringResource(R.string.version, localVersion))
                Text(stringResource(R.string.kernel_type, k.gkiType))
                
            }
        }
    }
}