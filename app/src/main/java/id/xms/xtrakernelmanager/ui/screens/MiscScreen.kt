package id.xms.xtrakernelmanager.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import id.xms.xtrakernelmanager.ui.theme.XtraTheme
import id.xms.xtrakernelmanager.viewmodel.MiscViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiscScreen(
    vm: MiscViewModel = hiltViewModel(),
    navController: NavController? = null,
    onBackPressed: () -> Unit = {} // Tambahkan parameter onBackPressed
) { // Awal fungsi MiscScreen
    BackHandler {
        onBackPressed()
    }
    XtraTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Miscellaneous") },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp), // Padding seragam
                verticalArrangement = Arrangement.Center, // Pusatkan konten secara vertikal
                horizontalAlignment = Alignment.CenterHorizontally // Pusatkan konten secara horizontal
            ) {
                Text(
                    text = "😀", // Emoji senyum besar
                    style = TextStyle(
                        fontSize = 64.sp, // Ukuran font emoji
 textAlign = TextAlign.Center
                    )
                )
                Spacer(modifier = Modifier.height(16.dp)) // Spasi antara emoji dan teks
                val fullText = "This feature is still under development."
                var displayedText by remember { mutableStateOf("") }

                LaunchedEffect(Unit) {
                    fullText.forEachIndexed { index, char ->
                        displayedText += char
                        delay(100) // Sesuaikan kecepatan mengetik di sini (dalam milidetik)
                    }
                }

                Text(
                    text = displayedText,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif, // Font yang lebih umum
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp, // Ukuran font lebih besar
                        color = MaterialTheme.colorScheme.onBackground,
 textAlign = TextAlign.Center
                    )
                )
                Spacer(modifier = Modifier.height(16.dp)) // Add some space

                val text1 = "Your support helps us bring new features like this to life faster!"
                var displayedText1 by remember { mutableStateOf("") }
                LaunchedEffect(Unit) {
                    text1.forEachIndexed { index, char ->
                        displayedText1 += char
                        delay(50) // Adjust typing speed here
                    }
                }
                Text(
                    text = displayedText1,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
 textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                val text2 = "Consider donating to keep the development going. Thank you!"
                var displayedText2 by remember { mutableStateOf("") }
                LaunchedEffect(Unit) {
                    // Start typing text2 after text1 has finished
                    delay(text1.length * 50L) // Wait for text1 to finish
                    text2.forEachIndexed { index, char ->
                        displayedText2 += char
                        delay(50) // Adjust typing speed here
                    }
                }
                Text(
                    text = displayedText2,
                    style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                )
            }
        }
    }
}