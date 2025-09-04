package id.xms.xtrakernelmanager.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.*
import dagger.hilt.android.AndroidEntryPoint
import id.xms.xtrakernelmanager.data.repository.ThermalRepository
import id.xms.xtrakernelmanager.service.ThermalService
import id.xms.xtrakernelmanager.ui.components.BottomNavBar
import id.xms.xtrakernelmanager.ui.dialog.BatteryOptDialog
import id.xms.xtrakernelmanager.ui.screens.*
import id.xms.xtrakernelmanager.ui.theme.XtraTheme
import id.xms.xtrakernelmanager.util.BatteryOptimizationChecker
import id.xms.xtrakernelmanager.util.LanguageManager
import id.xms.xtrakernelmanager.util.RootUtils
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var rootUtils: RootUtils

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    @Inject
    lateinit var thermalRepository: ThermalRepository

    @Inject
    lateinit var languageManager: LanguageManager

    private lateinit var batteryOptChecker: BatteryOptimizationChecker
    private var showBatteryOptDialog by mutableStateOf(false)
    private var permissionDenialCount by mutableStateOf(0)
    private val MAX_PERMISSION_RETRIES = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rootUtils.init(this)
        batteryOptChecker = BatteryOptimizationChecker(this)

        // Check permissions first before starting any services
        checkAndHandlePermissions()

        // Observe language changes
        lifecycleScope.launch {
            languageManager.currentLanguage.collect()
        }

        setContent {
            XtraTheme {
                val navController = rememberNavController()
                val items = listOf("Home", "Tuning", "Misc", "Info") // Removed Settings from here

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
                            batteryOptChecker.checkAndRequestPermissions(this)
                        },
                        onExit = { finish() },
                        showExitButton = permissionDenialCount >= MAX_PERMISSION_RETRIES
                    )
                }

                Scaffold(
                    bottomBar = { BottomNavBar(navController, items) }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("home") { HomeScreen(navController = navController) }
                        composable("tuning") { TuningScreen() }
                        composable("misc") { MiscScreen() }
                        composable("info") { InfoScreen() }
                        // Keep settings route for potential direct navigation
                        composable("settings") { SettingsScreen(navController = navController) }
                    }
                }
            }
        }
    }

    private fun checkAndHandlePermissions() {
        if (!batteryOptChecker.hasRequiredPermissions()) {
            showBatteryOptDialog = true
        } else {
            // Only start service if we have permissions
            startForegroundService(Intent(this, ThermalService::class.java))
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