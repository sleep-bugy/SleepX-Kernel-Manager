package id.xms.xtrakernelmanager.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import id.xms.xtrakernelmanager.ui.components.BatteryCard
import id.xms.xtrakernelmanager.ui.components.CpuClusterCard
import id.xms.xtrakernelmanager.ui.components.RootStatusCard
import id.xms.xtrakernelmanager.ui.components.SystemCard
import id.xms.xtrakernelmanager.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val homeViewModel: HomeViewModel = viewModel()
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()

    // Inisialisasi ViewModel dengan Context saat pertama kali
    androidx.compose.runtime.LaunchedEffect(Unit) {
        homeViewModel.initialize(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RootStatusCard(
            isRooted = uiState.isRooted,
            showEasterEgg = uiState.showEasterEgg,
            onClick = homeViewModel::onRootStatusClick
        )
        BatteryCard(batteryLevel = uiState.batteryLevel)
        CpuClusterCard(
            coreCount = uiState.cpuCores,
            maxFreq = uiState.maxFreq,
            minFreq = uiState.minFreq
        )
        SystemCard(
            kernelVersion = uiState.kernelVersion,
            usedRam = uiState.usedRam
        )
    }
}