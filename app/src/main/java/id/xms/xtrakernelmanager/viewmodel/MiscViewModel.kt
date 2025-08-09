package id.xms.xtrakernelmanager.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import id.xms.xtrakernelmanager.data.repository.RootRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class MiscViewModel @Inject constructor(
    private val rootRepo: RootRepository
) : ViewModel()

    private val _output = MutableStateFlow("")
    val output = _output.asStateFlow()

