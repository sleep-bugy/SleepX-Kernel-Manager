package id.xms.xtrakernelmanager.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import id.xms.xtrakernelmanager.data.model.ThemeType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

@Singleton
class ThemeRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val THEME_TYPE = stringPreferencesKey("theme_type")
    }

    val themeType: Flow<ThemeType> = context.dataStore.data.map { preferences ->
        val themeName = preferences[PreferencesKeys.THEME_TYPE] ?: ThemeType.GLASSMORPHISM.name
        ThemeType.valueOf(themeName)
    }

    suspend fun setThemeType(themeType: ThemeType) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_TYPE] = themeType.name
        }
    }
}
