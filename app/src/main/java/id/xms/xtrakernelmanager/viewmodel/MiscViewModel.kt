package id.xms.xtrakernelmanager.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.xms.xtrakernelmanager.service.BatteryStatsService
import id.xms.xtrakernelmanager.utils.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MiscViewModel @Inject constructor(
    private val application: Application,
    private val preferenceManager: PreferenceManager
) : AndroidViewModel(application) {

    private val _batteryStatsEnabled = MutableStateFlow(false)
    val batteryStatsEnabled: StateFlow<Boolean> = _batteryStatsEnabled.asStateFlow()

    private val _batteryNotificationEnabled = MutableStateFlow(false)
    val batteryNotificationEnabled: StateFlow<Boolean> = _batteryNotificationEnabled.asStateFlow()

    private val _showBatteryTempInStatusBar = MutableStateFlow(false)
    val showBatteryTempInStatusBar: StateFlow<Boolean> = _showBatteryTempInStatusBar.asStateFlow()

    private val _fpsMonitorEnabled = MutableStateFlow(false)
    val fpsMonitorEnabled: StateFlow<Boolean> = _fpsMonitorEnabled.asStateFlow()

    init {
        // Load saved preferences on init
        _batteryStatsEnabled.value = preferenceManager.getBatteryStatsEnabled()
        _batteryNotificationEnabled.value = preferenceManager.getBatteryStatsEnabled()
        _showBatteryTempInStatusBar.value = preferenceManager.getShowBatteryTempInStatusBar()
        _fpsMonitorEnabled.value = preferenceManager.getFpsMonitorEnabled()
    }

    fun toggleBatteryStats(enabled: Boolean) {
        viewModelScope.launch {
            _batteryStatsEnabled.value = enabled
            _batteryNotificationEnabled.value = enabled

            // Save preference for auto-start on boot - THIS IS THE KEY!
            preferenceManager.setBatteryStatsEnabled(enabled)

            if (enabled) {
                // Start the battery stats service
                val serviceIntent = Intent(application, BatteryStatsService::class.java)
                try {
                    application.startForegroundService(serviceIntent)
                } catch (e: Exception) {
                    // Handle service start error
                    _batteryStatsEnabled.value = false
                    _batteryNotificationEnabled.value = false
                    preferenceManager.setBatteryStatsEnabled(false)
                }
            } else {
                // Stop the battery stats service and disable auto-start
                val serviceIntent = Intent(application, BatteryStatsService::class.java)
                application.stopService(serviceIntent)
                preferenceManager.setBatteryStatsEnabled(false)
            }
        }
    }

    fun toggleShowBatteryTempInStatusBar(enabled: Boolean) {
        viewModelScope.launch {
            _showBatteryTempInStatusBar.value = enabled
            preferenceManager.setShowBatteryTempInStatusBar(enabled)
        }
    }

    fun toggleGameControl(enabled: Boolean) {
        viewModelScope.launch {
            _fpsMonitorEnabled.value = enabled
            preferenceManager.setFpsMonitorEnabled(enabled)

            // Always keep the service running to support per-app FPS monitoring
            // The service itself will decide what to show based on per-app settings
            val serviceIntent = Intent(application, Class.forName("id.xms.xtrakernelmanager.service.GameControlService"))
            if (enabled) {
                // Start service if not already running
                try {
                    application.startForegroundService(serviceIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            // Note: We no longer stop the service when global setting is disabled
            // This allows per-app FPS monitoring to work independently
        }
    }
}
