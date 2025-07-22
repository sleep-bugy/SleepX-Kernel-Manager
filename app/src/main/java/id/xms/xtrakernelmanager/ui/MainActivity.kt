package id.xms.xtrakernelmanager.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import id.xms.xtrakernelmanager.ui.components.BottomNavBar
import id.xms.xtrakernelmanager.ui.screens.HomeScreen
import id.xms.xtrakernelmanager.ui.theme.XtraKernelManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            XtraKernelManagerTheme {
                MainScreen()
            }
        }
    }
}

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