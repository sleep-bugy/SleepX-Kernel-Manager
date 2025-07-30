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

    val clusters = flow { emit(repo.getCpuClusters()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val battery = flow { emit(repo.getBatteryInfo()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val system = flow { emit(repo.getSystemInfo()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val gpu = flow { emit(repo.getGpuInfo()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val kernel = flow { emit(repo.getKernelInfo()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
}