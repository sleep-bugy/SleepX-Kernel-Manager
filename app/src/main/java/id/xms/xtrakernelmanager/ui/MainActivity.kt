package id.xms.xtrakernelmanager.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.*
import dagger.hilt.android.AndroidEntryPoint
import id.xms.xtrakernelmanager.data.repository.ThermalRepository
import id.xms.xtrakernelmanager.service.ThermalService
import id.xms.xtrakernelmanager.ui.components.BottomNavBar
import id.xms.xtrakernelmanager.ui.components.ExpressiveBackground
import id.xms.xtrakernelmanager.ui.dialog.BatteryOptDialog
import id.xms.xtrakernelmanager.ui.screens.*
import id.xms.xtrakernelmanager.ui.theme.XtraTheme
import id.xms.xtrakernelmanager.ui.viewmodel.ThemeViewModel
import id.xms.xtrakernelmanager.util.BatteryOptimizationChecker
import id.xms.xtrakernelmanager.util.LanguageManager
import id.xms.xtrakernelmanager.util.RootUtils
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {


    @Inject
    lateinit var dataStore: DataStore<Preferences>

    @Inject
    lateinit var thermalRepository: ThermalRepository

    @Inject
    lateinit var languageManager: LanguageManager

    private lateinit var batteryOptChecker: BatteryOptimizationChecker
    private var showBatteryOptDialog by mutableStateOf(false)
    private var permissionDenialCount by mutableIntStateOf(0)
    private val MAX_PERMISSION_RETRIES = 2
    private var showNonRootDialog by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force dark mode always - ignore system theme setting
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        enableEdgeToEdge() // Enable edge-to-edge display for Android 16-like experience
        batteryOptChecker = BatteryOptimizationChecker(this)

        // Check permissions first before starting any services
        checkAndHandlePermissions()

        // Cek root dan tampilkan dialog jika non-root
        if (!RootUtils.isDeviceRooted()) {
            showNonRootDialog = true
        }

        // Observe language changes and recreate activity when changed
        lifecycleScope.launch {
            languageManager.currentLanguage
                .drop(1) // skip initial to avoid immediate recreate
                .collect { recreate() }
        }

        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val currentTheme by themeViewModel.currentTheme.collectAsState()

            XtraTheme(themeType = currentTheme) {
                val navController = rememberNavController()
                val items = listOf("Home", "Tuning", "Misc", "Info")

                // Use Surface instead of ExpressiveBackground to avoid potential composition issues
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (showBatteryOptDialog) {
                        BatteryOptDialog(
                            onDismiss = {
                                // Only allow dismiss if we haven't exceeded retry limit
                                if (permissionDenialCount < MAX_PERMISSION_RETRIES) {
                                    showBatteryOptDialog = false
                                }
                            },
                            onConfirm = {
                                showBatteryOptDialog = false
                                batteryOptChecker.checkAndRequestPermissions(this@MainActivity)
                            },
                            onExit = { finish() },
                            showExitButton = permissionDenialCount >= MAX_PERMISSION_RETRIES
                        )
                    }

                    if (showNonRootDialog) {
                        AlertDialog(
                            onDismissRequest = { showNonRootDialog = false },
                            confirmButton = {
                                Button(onClick = { showNonRootDialog = false }) {
                                    Text("OK")
                                }
                            },
                            title = { Text("Non-Root Device Detected") },
                            text = { Text("You're using SleepX Manager without root access.\n" +
                                    "If you're rooted, allow it first with Magisk/KernelSU. If you're not, you can use Battery Notification and Clear Cache in Misc Features.") }
                        )
                    }

                    Scaffold(
                        containerColor = Color.Transparent,
                        bottomBar = { BottomNavBar(navController, items) },
                        modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars)
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = "home"
                            ) {
                                composable("home") { HomeScreen(navController = navController) }
                                composable("tuning") { TuningScreen() }
                                composable("misc") { MiscScreen(navController = navController) }
                                composable("info") { InfoScreen() }
                                composable("settings") { SettingsScreen(navController = navController) }
                                composable("app_selection") { AppSelectionScreen() }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startOtaUpdateListenerService() {
        val intent = Intent(this, id.xms.xtrakernelmanager.service.OtaUpdateListenerService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun checkAndHandlePermissions() {
        if (!batteryOptChecker.hasRequiredPermissions()) {
            showBatteryOptDialog = true
        } else {
            // Only start service if we have permissions
            startForegroundService(Intent(this, ThermalService::class.java))
            startOtaUpdateListenerService()
        }
    }

    override fun onResume() {
        super.onResume()
        // Check if permissions were denied
        if (!batteryOptChecker.hasRequiredPermissions()) {
            permissionDenialCount++
            if (permissionDenialCount >= MAX_PERMISSION_RETRIES) {
                // Show dialog with exit button after max retries
                showBatteryOptDialog = true
            } else if (!showBatteryOptDialog) {
                // Show normal dialog if not already showing
                showBatteryOptDialog = true
            }
        } else {
            // Reset counter if permissions are granted
            permissionDenialCount = 0
            showBatteryOptDialog = false

            // Ensure service is running if permissions are granted
            startForegroundService(Intent(this, ThermalService::class.java))
        }
    }
}
