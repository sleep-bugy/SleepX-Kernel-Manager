package id.xms.xtrakernelmanager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
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
            SuperGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                glassIntensity = GlassIntensity.Light
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header with gradient accent
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.kernel),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = stringResource(R.string.kernel_information),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Information sections with categorized layout
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                    ) {
                        // Basic Kernel Info Section
                        InfoSection(
                            title = "Kernel Details",
                            icon = Icons.Filled.Memory,
                            items = listOf(
                                InfoItem("Version", k.version),
                                InfoItem("Type", k.gkiType),
                                InfoItem("I/O Scheduler", k.scheduler)
                            )
                        )

                        // System Architecture Section
                        InfoSection(
                            title = "System Architecture",
                            icon = Icons.Filled.Computer,
                            items = listOf(
                                InfoItem("ABI", k.abi),
                                InfoItem("Architecture", k.architecture)
                            )
                        )

                        // Security Section
                        InfoSection(
                            title = "Security",
                            icon = Icons.Filled.Security,
                            items = listOf(
                                InfoItem("SELinux", k.selinuxStatus, getSelinuxColor(k.selinuxStatus)),
                                InfoItem("KernelSU", k.kernelSuStatus, getKernelSuColor(k.kernelSuStatus))
                            )
                        )
                    }

                    // Close button with modern style
                    FilledTonalButton(
                        onClick = { showDialog = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            text = "Close",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    SuperGlassCard(
        modifier = modifier,
        glassIntensity = GlassIntensity.Light
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Section with enhanced styling
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.kernel),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(40.dp)
                                .padding(8.dp)
                        )
                    }

                    Column {
                        Text(
                            text = stringResource(R.string.kernel),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "System Information",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Info button with enhanced styling
                FilledIconButton(
                    onClick = { showDialog = true },
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = stringResource(R.string.kernel_information),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Quick info grid with modern cards
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Process kernel version to extract clean version info
                val versionString = k.version
                val hashIndex = versionString.indexOf(" #")
                val parenIndex = versionString.indexOf(" (")
                val endIndex = when {
                    hashIndex != -1 && parenIndex != -1 -> minOf(hashIndex, parenIndex)
                    hashIndex != -1 -> hashIndex
                    parenIndex != -1 -> parenIndex
                    else -> versionString.length
                }
                val localVersion = versionString.substring(0, endIndex).trim()

                // Single card: Kernel Version (full width)
                CompactInfoCard(
                    label = stringResource(R.string.version),
                    value = localVersion,
                    icon = Icons.Filled.Memory,
                    modifier = Modifier.fillMaxWidth()
                )

                // Single card: GKI Type (full width)
                CompactInfoCard(
                    label = "Type",
                    value = k.gkiType,
                    icon = Icons.Filled.Computer,
                    modifier = Modifier.fillMaxWidth()
                )

                // Row: ABI and Architecture
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompactInfoCard(
                        label = "ABI",
                        value = k.abi.take(20) + if (k.abi.length > 20) "..." else "",
                        icon = Icons.Filled.Computer,
                        modifier = Modifier.weight(1f)
                    )
                    CompactInfoCard(
                        label = "Arch",
                        value = k.architecture.take(20) + if (k.architecture.length > 20) "..." else "",
                        icon = Icons.Filled.Memory,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row: SELinux and KernelSU (highlighted with colors)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompactInfoCard(
                        label = "SELinux",
                        value = k.selinuxStatus,
                        icon = Icons.Filled.Shield,
                        valueColor = getSelinuxColor(k.selinuxStatus),
                        modifier = Modifier.weight(1f)
                    )
                    CompactInfoCard(
                        label = "KernelSU",
                        value = when {
                            k.kernelSuStatus.contains("Version", ignoreCase = true) -> "✓ " + k.kernelSuStatus.substringAfter("Version ").take(8)
                            k.kernelSuStatus.contains("Active", ignoreCase = true) -> {
                                val inside = k.kernelSuStatus.substringAfter("(", "").substringBefore(")")
                                if (inside.isNotBlank()) "✓ $inside" else "✓ Active"
                            }
                            k.kernelSuStatus.contains("Detected", ignoreCase = true) -> "✓ Detected"
                            else -> "✗ Not Found"
                        },
                        icon = Icons.Filled.AdminPanelSettings,
                        valueColor = getKernelSuColor(k.kernelSuStatus),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactInfoCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    valueColor: Color? = null
) {
    SuperGlassCard(
        modifier = modifier,
        glassIntensity = GlassIntensity.Light
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Icon with colorful background
            Card(
                modifier = Modifier.size(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = getIconBackgroundColor(label)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = valueColor ?: MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun getIconBackgroundColor(label: String): Color {
    return when (label.lowercase()) {
        "version" -> Color(0xFF6366F1) // Indigo
        "type" -> Color(0xFF8B5CF6) // Purple
        "abi" -> Color(0xFF06B6D4) // Cyan
        "arch" -> Color(0xFF10B981) // Emerald
        "selinux" -> when {
            else -> Color(0xFFF59E0B) // Amber for security
        }
        "kernelsu" -> Color(0xFFEF4444) // Red
        else -> Color(0xFF6B7280) // Gray
    }
}

@Composable
private fun InfoSection(
    title: String,
    icon: ImageVector,
    items: List<InfoItem>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                thickness = 1.dp
            )

            // Section items
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items.forEach { item ->
                    InfoRow(
                        label = item.label,
                        value = item.value,
                        valueColor = item.valueColor
                    )
                }
            }
        }
    }
}

private data class InfoItem(
    val label: String,
    val value: String,
    val valueColor: Color? = null
)

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: Color? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.6f)
        )
    }
}

@Composable
private fun getSelinuxColor(status: String): Color {
    return when (status.lowercase()) {
        "enforcing" -> Color(0xFF4CAF50) // Green
        "permissive" -> Color(0xFFFF9800) // Orange
        "disabled" -> Color(0xFFF44336) // Red
        else -> MaterialTheme.colorScheme.onSurface
    }
}

@Composable
private fun getKernelSuColor(status: String): Color {
    return when {
        status.contains("Version", ignoreCase = true) -> Color(0xFF4CAF50) // Green for detected version
        status.contains("Active", ignoreCase = true) -> Color(0xFF4CAF50) // Green for active
        status.contains("Detected", ignoreCase = true) -> Color(0xFF2196F3) // Blue for detected
        status.contains("Not Detected", ignoreCase = true) -> Color(0xFF9E9E9E) // Gray
        else -> MaterialTheme.colorScheme.onSurface
    }
}
