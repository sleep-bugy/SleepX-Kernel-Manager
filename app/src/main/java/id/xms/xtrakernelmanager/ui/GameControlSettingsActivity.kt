package id.xms.xtrakernelmanager.ui

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import dagger.hilt.android.AndroidEntryPoint
import id.xms.xtrakernelmanager.ui.components.GlassIntensity
import id.xms.xtrakernelmanager.ui.components.SuperGlassCard
import id.xms.xtrakernelmanager.ui.theme.XtraTheme
import id.xms.xtrakernelmanager.viewmodel.GameControlViewModel

@AndroidEntryPoint
class GameControlSettingsActivity : ComponentActivity() {

    private val viewModel: GameControlViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if we were launched with a specific package
        val packageName = intent.getStringExtra("package_name") ?: ""

        setContent {
            XtraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (packageName.isNotEmpty()) {
                        // Show single app settings
                        AppGameControlSettings(packageName) {
                            finish()
                        }
                    } else {
                        // Show app list
                        GameControlAppList {
                            finish()
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun GameControlAppList(onBack: () -> Unit) {
        val context = LocalContext.current
        val searchQuery = remember { mutableStateOf("") }
        val appList by viewModel.appList.collectAsState()
        val enabledApps by viewModel.enabledApps.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()

        LaunchedEffect(Unit) {
            viewModel.loadInstalledApps(context)
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Game Control Settings") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Text(
                    text = "Select apps to enable Game Control",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium
                )

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(appList.filter { it.first.loadLabel(context.packageManager).contains(searchQuery.value, ignoreCase = true) }) { (appInfo, icon) ->
                            val packageName = appInfo.packageName
                            val isEnabled = enabledApps.any { it.packageName == packageName }

                            AppListItem(
                                appInfo = appInfo,
                                icon = icon,
                                isEnabled = isEnabled,
                                onClick = {
                                    if (isEnabled) {
                                        viewModel.disableGameControlForApp(packageName)
                                    } else {
                                        val intent = Intent(context, GameControlSettingsActivity::class.java)
                                        intent.putExtra("package_name", packageName)
                                        context.startActivity(intent)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun AppListItem(
        appInfo: ApplicationInfo,
        icon: Drawable,
        isEnabled: Boolean,
        onClick: () -> Unit
    ) {
        val context = LocalContext.current
        val appName = appInfo.loadLabel(context.packageManager).toString()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            Image(
                bitmap = icon.toBitmap().asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )

            // App Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = appName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = appInfo.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Enabled Indicator
            if (isEnabled) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Divider(
            modifier = Modifier.padding(start = 72.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AppGameControlSettings(packageName: String, onBack: () -> Unit) {
        val context = LocalContext.current
        val appSettings by viewModel.currentAppSettings.collectAsState()
        val pm = context.packageManager

        // Load app info
        val appInfo = remember {
            try {
                pm.getApplicationInfo(packageName, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
        }

        val appName = remember {
            appInfo?.loadLabel(pm)?.toString() ?: packageName
        }

        val appIcon = remember {
            appInfo?.loadIcon(pm) ?: context.getDrawable(android.R.drawable.sym_def_app_icon)
        }

        LaunchedEffect(packageName) {
            viewModel.loadAppSettings(packageName)
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Game Control: $appName") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // App Info Card
                SuperGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    glassIntensity = GlassIntensity.Light
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            bitmap = appIcon?.toBitmap()?.asImageBitmap()
                                ?: androidx.compose.ui.graphics.ImageBitmap.Companion.imageResource(
                                    id = android.R.drawable.sym_def_app_icon
                                ),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 16.dp)
                        ) {
                            Text(
                                text = appName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Main Settings Card
                SuperGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    glassIntensity = GlassIntensity.Light
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Enable Game Control
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enable Game Control",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Activate game control overlay for this app",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Switch(
                                checked = appSettings?.enabled ?: false,
                                onCheckedChange = { enabled ->
                                    viewModel.updateAppSettings(packageName) { it.copy(enabled = enabled) }
                                }
                            )
                        }

                        Divider()

                        // Default Performance Mode
                        Text(
                            text = "Default Performance Mode",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val currentMode = appSettings?.defaultPerformanceMode ?: "default"

                            PerformanceModeButton(
                                text = "Default",
                                isSelected = currentMode == "default"
                            ) {
                                viewModel.updateAppSettings(packageName) {
                                    it.copy(defaultPerformanceMode = "default")
                                }
                            }

                            PerformanceModeButton(
                                text = "Battery",
                                isSelected = currentMode == "battery"
                            ) {
                                viewModel.updateAppSettings(packageName) {
                                    it.copy(defaultPerformanceMode = "battery")
                                }
                            }

                            PerformanceModeButton(
                                text = "Performance",
                                isSelected = currentMode == "performance"
                            ) {
                                viewModel.updateAppSettings(packageName) {
                                    it.copy(defaultPerformanceMode = "performance")
                                }
                            }
                        }

                        Divider()

                        // Auto-enable DND
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-enable Do Not Disturb",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Automatically enable DND when app launches",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Switch(
                                checked = appSettings?.dndEnabled ?: false,
                                onCheckedChange = { enabled ->
                                    viewModel.updateAppSettings(packageName) { it.copy(dndEnabled = enabled) }
                                }
                            )
                        }

                        // Auto-clear background apps
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Clear Background Apps",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Clear background apps when launching",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Switch(
                                checked = appSettings?.clearBackgroundOnLaunch ?: false,
                                onCheckedChange = { enabled ->
                                    viewModel.updateAppSettings(packageName) {
                                        it.copy(clearBackgroundOnLaunch = enabled)
                                    }
                                }
                            )
                        }

                        Divider()

                        // Display options
                        Text(
                            text = "Display Options",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )

                        // Show FPS Counter
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Show FPS Counter",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Display current FPS on screen",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Switch(
                                checked = appSettings?.showFpsCounter ?: true,
                                onCheckedChange = { enabled ->
                                    viewModel.updateAppSettings(packageName) { it.copy(showFpsCounter = enabled) }
                                }
                            )
                        }

                        // Show Full Overlay
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-show Full Overlay",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Automatically show full overlay when app launches",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Switch(
                                checked = appSettings?.showFullOverlay ?: false,
                                onCheckedChange = { enabled ->
                                    viewModel.updateAppSettings(packageName) { it.copy(showFullOverlay = enabled) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun PerformanceModeButton(
        text: String,
        isSelected: Boolean,
        onClick: () -> Unit
    ) {
        val backgroundColor = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface
        }

        val textColor = if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        }

        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = backgroundColor,
                contentColor = textColor
            )
        ) {
            Text(text)
        }
    }
}
