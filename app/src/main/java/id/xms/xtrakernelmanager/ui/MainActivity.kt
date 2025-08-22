package id.xms.xtrakernelmanager.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.*
import dagger.hilt.android.AndroidEntryPoint
import id.xms.xtrakernelmanager.ui.components.BottomNavBar
import id.xms.xtrakernelmanager.ui.screens.*
import id.xms.xtrakernelmanager.ui.theme.XtraTheme
import id.xms.xtrakernelmanager.util.RootUtils
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var rootUtils: RootUtils

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    private val LANGUAGE_KEY = stringPreferencesKey("app_language")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rootUtils.init(this)

        lifecycleScope.launch {
            dataStore.data.map { preferences ->
                preferences[LANGUAGE_KEY] ?: "en"
            }.collect { language ->
                val currentLocale = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    resources.configuration.locales.get(0).language
                } else {
                    @Suppress("DEPRECATION")
                    resources.configuration.locale.language
                }

                if (currentLocale != language) {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
                }
            }
        }

        setContent {
            XtraTheme {
                val navController = rememberNavController()
                val items = listOf("Home", "Tuning", "Misc", "Info")

                Scaffold(
                    bottomBar = { BottomNavBar(navController, items) }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("home") { HomeScreen(navController = navController) }
                        composable("tuning") { TuningScreen() }
                        composable("misc") { MiscScreen() }
                        composable("info") { InfoScreen() }
                        composable("settings") { SettingsScreen(navController = navController) }
                    }
                }
            }
        }
    }
}