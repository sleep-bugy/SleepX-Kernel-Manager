package id.xms.xtrakernelmanager.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Info
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

    SuperGlassCard(
        modifier = modifier,
        glassIntensity = GlassIntensity.Light
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = stringResource(id = R.string.about),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Description
            Text(
                text = stringResource(id = R.string.desc_about),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
            )

            // Action Section
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Social Links Row
                val uriHandler = LocalUriHandler.current
                val donateLink = stringResource(id = R.string.donate_link)
                val donateText = stringResource(id = R.string.donate)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Follow us:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )

                    // Telegram Button
                    FilledTonalIconButton(
                        onClick = { uriHandler.openUri(telegramLink) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.telegram),
                            contentDescription = stringResource(id = R.string.telegram),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // GitHub Button
                    FilledTonalIconButton(
                        onClick = { uriHandler.openUri(githubLink) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.github),
                            contentDescription = stringResource(id = R.string.github),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Donate Button
                    FilledTonalButton(onClick = { uriHandler.openUri(donateLink) }) {
                        Text(text = donateText)
                    }
                }

                // Credits Badge
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )

                    AssistChip(
                        onClick = { showCreditsDialog = true },
                        label = {
                            Text(
                                text = stringResource(id = R.string.credits),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
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
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(stringResource(id = R.string.credits))
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        developers.forEach { developer ->
                            DeveloperCreditItem(developer = developer)
                        }
                    }
                },
                confirmButton = {
                    FilledTonalButton(onClick = { showCreditsDialog = false }) {
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { uriHandler.openUri(githubProfileUrl) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Image(
                painter = painterResource(id = developer.drawableResId),
                contentDescription = "${developer.name}'s profile picture",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = developer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = developer.role,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "@${developer.githubUsername}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
