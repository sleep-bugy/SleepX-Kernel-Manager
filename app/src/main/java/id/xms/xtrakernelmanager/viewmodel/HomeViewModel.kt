package id.xms.xtrakernelmanager.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log // Import Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.xms.xtrakernelmanager.data.model.*
import id.xms.xtrakernelmanager.data.repository.RootRepository
import id.xms.xtrakernelmanager.data.repository.SystemRepository
import id.xms.xtrakernelmanager.model.UpdateInfo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val systemRepo: SystemRepository,
    private val rootRepo: RootRepository
) : ViewModel() {


    private val _cpuInfo = MutableStateFlow(
        RealtimeCpuInfo(cores = 0, governor = "N/A", freqs = emptyList(), temp = 0f, soc = "N/A", cpuLoadPercentage = null)
    )
    val cpuInfo: StateFlow<RealtimeCpuInfo> = _cpuInfo.asStateFlow()

    private val _batteryInfo = MutableStateFlow<BatteryInfo?>(null)
    val batteryInfo: StateFlow<BatteryInfo?> = _batteryInfo.asStateFlow()

    private val _memoryInfo = MutableStateFlow<MemoryInfo?>(null)
    val memoryInfo: StateFlow<MemoryInfo?> = _memoryInfo.asStateFlow()

    private val _deepSleep = MutableStateFlow<DeepSleepInfo?>(null)
    val deepSleep: StateFlow<DeepSleepInfo?> = _deepSleep.asStateFlow()


    private val _kernelInfo = MutableStateFlow<KernelInfo?>(null)
    val kernelInfo: StateFlow<KernelInfo?> = _kernelInfo.asStateFlow()

    private val _rootStatus = MutableStateFlow<Boolean?>(null)
    val rootStatus: StateFlow<Boolean?> = _rootStatus.asStateFlow()

    private val _appVersion = MutableStateFlow<String?>("N/A")
    val appVersion: StateFlow<String?> = _appVersion.asStateFlow()

    private val _systemInfo = MutableStateFlow<SystemInfo?>(null)
    val systemInfo: StateFlow<SystemInfo?> = _systemInfo.asStateFlow()

    private val _isTitleAnimationDone = MutableStateFlow(false)
    val isTitleAnimationDone: StateFlow<Boolean> = _isTitleAnimationDone.asStateFlow()

    fun onTitleAnimationFinished() {
        _isTitleAnimationDone.value = true
    }

    private val _cpuClusters = MutableStateFlow<List<CpuCluster>>(emptyList())

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    private var lastShownVersion: String? = null

    private fun checkForUpdate() {
        val dbRef = FirebaseDatabase.getInstance().getReference("update")
        dbRef.get().addOnSuccessListener { snapshot ->
            val update = snapshot.getValue(UpdateInfo::class.java)
            val localVersion = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
            } catch (e: Exception) { "" }
            if (update != null && update.version.isNotBlank() && isNewerVersion(update.version, localVersion)) {
                if (lastShownVersion != update.version) {
                    _updateInfo.value = update
                    lastShownVersion = update.version
                }
            } else {
                _updateInfo.value = null
            }
        }.addOnFailureListener {
            _updateInfo.value = null
        }
    }

    private fun isNewerVersion(remote: String, local: String): Boolean {
        // Only compare numbers, ignore suffixes like -Release
        val remoteParts = remote.split(".", "-").mapNotNull { it.toIntOrNull() }
        val localParts = local.split(".", "-").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(remoteParts.size, localParts.size)) {
            val r = remoteParts.getOrNull(i) ?: 0
            val l = localParts.getOrNull(i) ?: 0
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }

    init {
        viewModelScope.launch {
            systemRepo.realtimeAggregatedInfoFlow
                .catch { e ->
                    Log.e("HomeViewModel", "Error in realtimeAggregatedInfoFlow: ${e.message}", e)

                }
                .collect { aggregatedInfo ->
                    _cpuInfo.value = aggregatedInfo.cpuInfo
                    _batteryInfo.value = aggregatedInfo.batteryInfo
                    _memoryInfo.value = aggregatedInfo.memoryInfo
                    _deepSleep.value = DeepSleepInfo(
                        uptime = aggregatedInfo.uptimeMillis,
                        deepSleep = aggregatedInfo.deepSleepMillis
                    )
                }
        }

        viewModelScope.launch {
            _systemInfo.value = systemRepo.getSystemInfo()
            _kernelInfo.value = systemRepo.getKernelInfo()
            _rootStatus.value = rootRepo.isRooted()
            _cpuClusters.value = systemRepo.getCpuClusters()

            try {
                @SuppressLint("PackageManagerGetSignatures")
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                _appVersion.value = pInfo.versionName
            } catch (e: Exception) {
                _appVersion.value = "N/A"
                Log.e("HomeViewModel", "Error getting app version", e)
            }
        }

        checkForUpdate()
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("HomeViewModel", "onCleared called.")
    }
}
