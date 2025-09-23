package id.xms.xtrakernelmanager.ui.viewmodel

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.xms.xtrakernelmanager.util.RootUtils
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AppFilter {
    ALL, SYSTEM, NON_SYSTEM
}

@HiltViewModel
class DeveloperOptionsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _allApps = mutableStateOf<List<AppInfo>>(emptyList())

    private val _selectedApps = mutableStateOf<Set<String>>(emptySet())
    val selectedApps: State<Set<String>> = _selectedApps

    private val _filter = mutableStateOf(AppFilter.ALL)
    val filter: State<AppFilter> = _filter

    private val _hideDeveloperOptionsEnabled = mutableStateOf(false)
    val hideDeveloperOptionsEnabled: State<Boolean> = _hideDeveloperOptionsEnabled

    val filteredApps: State<List<AppInfo>> = derivedStateOf {
        when (_filter.value) {
            AppFilter.ALL -> _allApps.value
            AppFilter.SYSTEM -> _allApps.value.filter { (it.info.flags and ApplicationInfo.FLAG_SYSTEM) != 0 }
            AppFilter.NON_SYSTEM -> _allApps.value.filter { (it.info.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
        }
    }

    private val HIDDEN_DEV_OPTS_APPS = stringSetPreferencesKey("hidden_dev_opts_apps")
    private val HIDE_DEV_OPTS_ENABLED = booleanPreferencesKey("hide_dev_opts_enabled")

    init {
        loadInstalledApps()
        loadSelectedApps()
        loadHideDeveloperOptionsState()
    }

    fun setFilter(newFilter: AppFilter) {
        _filter.value = newFilter
    }

    fun setHideDeveloperOptions(enabled: Boolean) {
        _hideDeveloperOptionsEnabled.value = enabled
        viewModelScope.launch {
            dataStore.edit { settings ->
                settings[HIDE_DEV_OPTS_ENABLED] = enabled
            }
        }
    }

    private fun loadHideDeveloperOptionsState() {
        viewModelScope.launch {
            dataStore.data.map { preferences ->
                preferences[HIDE_DEV_OPTS_ENABLED] ?: false
            }.collect { isEnabled ->
                _hideDeveloperOptionsEnabled.value = isEnabled
            }
        }
    }

    private fun loadInstalledApps() {
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        _allApps.value = installedApps.map { appInfo ->
            AppInfo(
                info = appInfo,
                label = appInfo.loadLabel(pm).toString(),
                packageName = appInfo.packageName,
                icon = appInfo.loadIcon(pm)
            )
        }.sortedBy { it.label }
    }

    private fun loadSelectedApps() {
        viewModelScope.launch {
            dataStore.data.map { preferences ->
                preferences[HIDDEN_DEV_OPTS_APPS] ?: emptySet()
            }.collect { 
                _selectedApps.value = it
            }
        }
    }

    fun toggleAppSelection(packageName: String) {
        viewModelScope.launch {
            val currentSelection = _selectedApps.value
            val isSelected = currentSelection.contains(packageName)

            val command = if (isSelected) {
                "pm grant $packageName android.permission.DUMP"
            } else {
                "pm revoke $packageName android.permission.DUMP"
            }

            RootUtils.runCommandAsRoot(command)

            dataStore.edit { settings ->
                val newSelection = if (isSelected) {
                    currentSelection - packageName
                } else {
                    currentSelection + packageName
                }
                settings[HIDDEN_DEV_OPTS_APPS] = newSelection
            }
        }
    }
}

data class AppInfo(
    val info: ApplicationInfo,
    val label: String,
    val packageName: String,
    val icon: android.graphics.drawable.Drawable
)
