package id.xms.xtrakernelmanager.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.xms.xtrakernelmanager.data.repository.SystemRepository
import id.xms.xtrakernelmanager.data.repository.RootRepository
import id.xms.xtrakernelmanager.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: SystemRepository,
    private val rootRepo: RootRepository
) : ViewModel() {

    private val _cpuInfo = MutableStateFlow(RealtimeCpuInfo(0, "", emptyList(), 0f))
    val cpuInfo: StateFlow<RealtimeCpuInfo> = _cpuInfo

    init {
        viewModelScope.launch {
            while (isActive) {
                _cpuInfo.value = repo.getCpuRealtime()
                delay(1000L)
            }
        }
    }

    val batteryInfo = repo.getBatteryInfo()
    val memoryInfo  = repo.getMemoryInfo()
    val kernelInfo  = repo.getKernelInfo()
    val rootStatus  = rootRepo.isRooted()
    val appVersion  = context.packageManager
        .getPackageInfo(context.packageName, 0).versionName ?: "1.0"

    private val _deepSleep = MutableStateFlow(repo.getDeepSleepInfo())
    val deepSleep: StateFlow<DeepSleepInfo> = _deepSleep
}