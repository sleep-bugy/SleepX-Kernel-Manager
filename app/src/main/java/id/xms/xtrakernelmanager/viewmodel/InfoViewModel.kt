package id.xms.xtrakernelmanager.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.xms.xtrakernelmanager.data.repository.SystemRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class InfoViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: SystemRepository
) : ViewModel() {

    val kernel = flow { emit(repo.getKernelInfo()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val system = flow { emit(repo.getSystemInfo()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
}