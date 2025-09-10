package id.xms.xtrakernelmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.xms.xtrakernelmanager.data.model.ThemeType
import id.xms.xtrakernelmanager.data.repository.ThemeRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeRepository: ThemeRepository
) : ViewModel() {

    val currentTheme: StateFlow<ThemeType> = themeRepository.themeType.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = if (android.os.Build.VERSION.SDK_INT >= 31) ThemeType.MATERIAL3 else ThemeType.GLASSMORPHISM
    )

    fun setTheme(themeType: ThemeType) {
        viewModelScope.launch {
            themeRepository.setThemeType(themeType)
        }
    }
}
