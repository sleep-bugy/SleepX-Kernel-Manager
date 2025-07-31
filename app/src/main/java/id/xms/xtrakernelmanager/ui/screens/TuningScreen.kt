package id.xms.xtrakernelmanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import id.xms.xtrakernelmanager.ui.components.CpuGovernorCard
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
                    Text("Tuning Control", style = TextStyle(fontSize = 27.sp))
                }
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            /* 1. CPU Control */
            CpuGovernorCard(
                vm = viewModel,
                blur = true
            )

            /* 2. Thermal */
            // ThermalCard sekarang hanya memerlukan ViewModel, pastikan definisinya benar
            ThermalCard(
                viewModel = viewModel, // Teruskan instance ViewModel
                blur = true
            )

            /* 3. GPU Control (Contoh jika Anda memiliki GpuCard) */
            // Jika GpuCard masih menggunakan 'vm':
            // GpuCard(
            //     vm = viewModel,
            //     blur = true
            // )
            // Jika GpuCard sudah diupdate menggunakan 'viewModel':
            // GpuCard(
            //     viewModel = viewModel,
            //     blur = true
            // )


            /* 4. Swappiness */
            SwappinessCard(
                value = swappiness,
                onValueChange = viewModel::setSwappiness,
                blur = true
            )

            // Anda bisa menambahkan kartu lain di sini sesuai kebutuhan
        }
    }
}

