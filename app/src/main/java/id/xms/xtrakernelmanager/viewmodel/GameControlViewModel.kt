package id.xms.xtrakernelmanager.viewmodel

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.xms.xtrakernelmanager.data.model.AppGameSettings
import id.xms.xtrakernelmanager.data.repository.GameControlRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class GameControlViewModel @Inject constructor(
    private val gameControlRepository: GameControlRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _appList = MutableStateFlow<List<Pair<ApplicationInfo, Drawable>>>(emptyList())
    val appList: StateFlow<List<Pair<ApplicationInfo, Drawable>>> = _appList.asStateFlow()

    private val _enabledApps = MutableStateFlow<List<AppGameSettings>>(emptyList())
    val enabledApps: StateFlow<List<AppGameSettings>> = _enabledApps.asStateFlow()

    private val _currentAppSettings = MutableStateFlow<AppGameSettings?>(null)
    val currentAppSettings: StateFlow<AppGameSettings?> = _currentAppSettings.asStateFlow()

    init {
        loadEnabledApps()
    }

    fun loadInstalledApps(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _appList.value = withContext(Dispatchers.IO) {
                val pm = context.packageManager
                val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                installedApps
                    .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
                    .map { appInfo ->
                        val icon = appInfo.loadIcon(pm)
                        Pair(appInfo, icon)
                    }
                    .sortedBy { it.first.loadLabel(pm).toString().lowercase() }
            }
            _isLoading.value = false
        }
    }

    fun loadEnabledApps() {
        viewModelScope.launch {
            _enabledApps.value = gameControlRepository.getAllEnabledApps()
        }
    }

    fun loadAppSettings(packageName: String) {
        viewModelScope.launch {
            val settings = gameControlRepository.getAppSettings(packageName)
            if (settings != null) {
                _currentAppSettings.value = settings
            } else {
                _currentAppSettings.value = AppGameSettings(packageName)
            }
        }
    }

    fun updateAppSettings(packageName: String, updateBlock: (AppGameSettings) -> AppGameSettings) {
        viewModelScope.launch {
            val current = _currentAppSettings.value ?: AppGameSettings(packageName)
            val updated = updateBlock(current)

            // Save to repository
            gameControlRepository.saveAppSettings(updated)

            // Update state
            _currentAppSettings.value = updated

            // Update enabled apps list if this app is enabled/disabled
            if (updated.enabled) {
                val currentList = _enabledApps.value.toMutableList()
                val index = currentList.indexOfFirst { it.packageName == packageName }
                if (index >= 0) {
                    currentList[index] = updated
                } else {
                    currentList.add(updated)
                }
                _enabledApps.value = currentList
            } else {
                _enabledApps.value = _enabledApps.value.filter { it.packageName != packageName }
            }
        }
    }

    fun disableGameControlForApp(packageName: String) {
        viewModelScope.launch {
            val settings = gameControlRepository.getAppSettings(packageName) ?: AppGameSettings(packageName)
            val updated = settings.copy(enabled = false)
            gameControlRepository.saveAppSettings(updated)

            // Update enabled apps list
            _enabledApps.value = _enabledApps.value.filter { it.packageName != packageName }
        }
    }
}
