package id.xms.xtrakernelmanager.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.xms.xtrakernelmanager.R

data class Developer(val name: String, val role: String, val githubUsername: String, val drawableResId: Int)

val developers = listOf(
    Developer("Gustyx-Power", "Founder & Developer", "Gustyx-Power", R.drawable.gustyx_power),
    Developer("Pavelc4", "Ui Supports", "pavelc4", R.drawable.pavelc4),
    Developer("Ziyu", "Tuning Supports", "Ziyu", R.drawable.ziyu)
)


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
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
        AnimatedVisibility(
            visible = showCreditsDialog,
            enter = fadeIn(animationSpec = tween(durationMillis = 300)),
        ) {
            AlertDialog(
                onDismissRequest = { showCreditsDialog = false },
                title = { Text(stringResource(id = R.string.credits)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        developers.forEach { developer ->
                            DeveloperCreditItem(developer = developer)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCreditsDialog = false }) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            )
        }
    }
}

@Composable
fun DeveloperCreditItem(developer: Developer) {
    val uriHandler = LocalUriHandler.current
    val githubProfileUrl = "https://github.com/${developer.githubUsername}"
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Image(
            painter = painterResource(id = developer.drawableResId),
            contentDescription = "${developer.name}'s profile picture",
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable { uriHandler.openUri(githubProfileUrl) }
        )

        Column {
            Text(text = developer.name, style = MaterialTheme.typography.titleMedium)
            Text(text = developer.role, style = MaterialTheme.typography.bodySmall)
            Text(
                text = "@${developer.githubUsername}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { uriHandler.openUri(githubProfileUrl) }
            )
        }
    }
}

