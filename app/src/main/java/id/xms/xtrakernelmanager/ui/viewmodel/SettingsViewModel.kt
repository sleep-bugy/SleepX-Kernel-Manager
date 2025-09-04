package id.xms.xtrakernelmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.xms.xtrakernelmanager.util.Language
import id.xms.xtrakernelmanager.util.LanguageManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val languageManager: LanguageManager
) : ViewModel() {

    val currentLanguage: StateFlow<Language> = languageManager.currentLanguage
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            Language.ENGLISH
        )

    fun setLanguage(language: Language) {
        viewModelScope.launch {
            languageManager.setLanguage(language)
        }
    }
}
