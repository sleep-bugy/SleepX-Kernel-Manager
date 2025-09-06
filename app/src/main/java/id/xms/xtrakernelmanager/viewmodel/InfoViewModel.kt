package id.xms.xtrakernelmanager.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.xms.xtrakernelmanager.data.repository.SystemRepository
import id.xms.xtrakernelmanager.data.model.KernelInfo
import id.xms.xtrakernelmanager.data.model.SystemInfo
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class InfoViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: SystemRepository
) : ViewModel() {

    val kernel: StateFlow<KernelInfo?> = flow { emit(repo.getKernelInfo()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val system: StateFlow<SystemInfo?> = flow { emit(repo.getSystemInfo()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
}