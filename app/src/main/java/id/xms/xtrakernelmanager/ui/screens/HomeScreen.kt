package id.xms.xtrakernelmanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import id.xms.xtrakernelmanager.ui.components.*
import id.xms.xtrakernelmanager.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: HomeViewModel = hiltViewModel()) {
    val cpuInfo by vm.cpuInfo.collectAsState()
    val battery = vm.batteryInfo
    val memory = vm.memoryInfo
    val deepSleep by vm.deepSleep.collectAsState()
    val root = vm.rootStatus
    val kernel = vm.kernelInfo
    val version = vm.appVersion
    var blurOn by rememberSaveable { mutableStateOf(true) }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(32.dp)
            ) {

                TopAppBar(
                    title = { Text("Xtra Kernel Manager") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (blurOn) Color.Transparent else MaterialTheme.colorScheme.surface
                    ),
                    actions = {
                        IconToggleButton(
                            checked = blurOn,
                            onCheckedChange = { blurOn = it },
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Icon(
                                imageVector = if (blurOn) Icons.Default.BlurOn else Icons.Default.BlurOff,
                                contentDescription = "Toggle blur"
                            )
                        }
                    }
                )
            }
        }
    ) { padding ->

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            /* 1. CPU */
            CpuCard(cpuInfo, blurOn)

            /* 2. Merged card */
            MergedSystemCard(battery, deepSleep, root, version, blurOn, memory)

            /* 3. Kernel */
            KernelCard(kernel, blurOn)

            /* 4. About */
            AboutCard(blurOn)
        }
    }
}
