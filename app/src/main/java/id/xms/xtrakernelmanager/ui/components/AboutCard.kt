package id.xms.xtrakernelmanager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.xms.xtrakernelmanager.R

@Composable
fun AboutCard(
    blur: Boolean,
    modifier: Modifier = Modifier,
    githubLink: String = stringResource(R.string.github_link),
    telegramLink: String = stringResource(R.string.telegram_link),
) {
    var showCreditsDialog by remember { mutableStateOf(false) }
    GlassCard(blur, modifier) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    stringResource(id = R.string.about),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(stringResource(id = R.string.desc_about))
                val uriHandler = LocalUriHandler.current
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconButton(onClick = { uriHandler.openUri(telegramLink) }) {
                        Icon(
                            painterResource(id = R.drawable.telegram),
                            stringResource(id = R.string.telegram)
                        )
                    }
                    IconButton(onClick = { uriHandler.openUri(githubLink) }) {
                        Icon(
                            painterResource(id = R.drawable.github),
                            stringResource(id = R.string.github)
                        )
                    }
                }
                Badge(
                    modifier = Modifier
                        .clickable { showCreditsDialog = true }
                        .align(Alignment.Start)
                ) {
                    Text(
                        stringResource(id = R.string.credits).uppercase(),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

            }
        }
    }
    if (showCreditsDialog) {
        AlertDialog(
            onDismissRequest = { showCreditsDialog = false },
            title = { Text(stringResource(id = R.string.credits)) },
            text = {
                Text(stringResource(id = R.string.credits_author))
            },
            confirmButton = {
                TextButton(onClick = { showCreditsDialog = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )
    }
}
