package id.xms.xtrakernelmanager.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import dagger.hilt.android.AndroidEntryPoint
import id.xms.xtrakernelmanager.ui.components.BottomNavBar
import id.xms.xtrakernelmanager.ui.screens.*
import id.xms.xtrakernelmanager.ui.theme.XtraTheme
import id.xms.xtrakernelmanager.util.RootUtils
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var rootUtils: RootUtils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rootUtils.init(this)

        setContent {
            XtraTheme {
                val navController = rememberNavController()
                val items = listOf("Home", "Tuning", "Terminal", "Info")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController = rememberNavController()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Xtra Kernel Manager",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Start
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            BottomNavBar(navController = navController)
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.fillMaxSize()
        ) {
            composable("home") { HomeScreen(paddingValues = paddingValues) }
            // Placeholder untuk tab lain (akan diimplementasi nanti)
            composable("tuning") { EmptyScreen(paddingValues) }
            composable("terminal") { EmptyScreen(paddingValues) }
            composable("info") { EmptyScreen(paddingValues) }
        }
    }
}

@Composable
fun EmptyScreen(paddingValues: PaddingValues) {
    androidx.compose.material3.Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        color = MaterialTheme.colorScheme.background
    ) {}
}