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

    val battery = flow { emit(repo.getBatteryInfo()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val system = flow { emit(repo.getSystemInfo()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    private val _deepSleep = MutableStateFlow(repo.getDeepSleepInfo())
    val deepSleep: StateFlow<DeepSleepInfo> = _deepSleep
}