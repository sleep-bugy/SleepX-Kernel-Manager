package id.xms.xtrakernelmanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue // Pastikan ini ada
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle // Anda menggunakan ini, jadi kita pertahankan
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp // Anda menggunakan ini, jadi kita pertahankan
import androidx.hilt.navigation.compose.hiltViewModel
import id.xms.xtrakernelmanager.ui.components.CpuGovernorCard
import id.xms.xtrakernelmanager.ui.components.GpuControlCard // <-- IMPORT GpuControlCard
import id.xms.xtrakernelmanager.ui.components.ThermalCard
import id.xms.xtrakernelmanager.ui.components.SwappinessCard
import id.xms.xtrakernelmanager.viewmodel.TuningViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TuningScreen(viewModel: TuningViewModel = hiltViewModel()) {
    val swappiness by viewModel.swappiness.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Mempertahankan style teks kustom Anda
                    Text("Tuning Control", style = TextStyle(fontSize = 27.sp))
                }
            )
        }
    ) { paddingValues -> // innerPadding dari Scaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues) // Terapkan innerPadding dari Scaffold
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            /* 1. CPU Control */
            CpuGovernorCard(
                vm = viewModel, // Mempertahankan parameter 'vm'
                blur = true
            )

            /* 2. GPU Control */ // <-- TAMBAHKAN GpuControlCard DI SINI
            GpuControlCard(
                tuningViewModel = viewModel,
                blur = true // Tambahkan jika GpuControlCard Anda mendukung parameter blur
            )

            /* 3. Thermal */
            ThermalCard(
                viewModel = viewModel,
                blur = true
            )

            /* 4. Swappiness */
            SwappinessCard(
                value = swappiness,
                onValueChange = viewModel::setSwappiness,
                blur = true
            )
        }
    }
}

