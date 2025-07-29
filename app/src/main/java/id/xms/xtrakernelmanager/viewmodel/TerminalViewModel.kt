package id.xms.xtrakernelmanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.xms.xtrakernelmanager.data.repository.RootRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val rootRepo: RootRepository
) : ViewModel() {

    private val _output = MutableStateFlow("")
    val output = _output.asStateFlow()

    fun exec(cmd: String) {
        viewModelScope.launch {
            val res = rootRepo.run(cmd)
            _output.value += "> $cmd\n$res\n"
        }
    }
}