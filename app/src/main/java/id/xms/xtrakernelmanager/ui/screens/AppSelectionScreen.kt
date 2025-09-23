package id.xms.xtrakernelmanager.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import id.xms.xtrakernelmanager.ui.viewmodel.AppFilter
import id.xms.xtrakernelmanager.ui.viewmodel.AppInfo
import id.xms.xtrakernelmanager.ui.viewmodel.DeveloperOptionsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionScreen(
    viewModel: DeveloperOptionsViewModel = hiltViewModel()
) {
    val filteredApps by viewModel.filteredApps
    val selectedApps by viewModel.selectedApps
    val currentFilter by viewModel.filter

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Select Apps") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            FilterChips(
                selectedFilter = currentFilter,
                onFilterSelected = { newFilter -> viewModel.setFilter(newFilter) }
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(items = filteredApps, key = { it.packageName }) { app ->
                    AppListItem(
                        app = app,
                        isSelected = selectedApps.contains(app.packageName),
                        onAppSelected = { viewModel.toggleAppSelection(app.packageName) }
                    )
                }
            }
        }
    }
}

@Composable
fun FilterChips(selectedFilter: AppFilter, onFilterSelected: (AppFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedFilter == AppFilter.ALL,
            onClick = { onFilterSelected(AppFilter.ALL) },
            label = { Text("All") }
        )
        FilterChip(
            selected = selectedFilter == AppFilter.SYSTEM,
            onClick = { onFilterSelected(AppFilter.SYSTEM) },
            label = { Text("System") }
        )
        FilterChip(
            selected = selectedFilter == AppFilter.NON_SYSTEM,
            onClick = { onFilterSelected(AppFilter.NON_SYSTEM) },
            label = { Text("Non-System") }
        )
    }
}

@Composable
fun AppListItem(
    app: AppInfo,
    isSelected: Boolean,
    onAppSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAppSelected() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberDrawablePainter(drawable = app.icon),
            contentDescription = null,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = app.label, style = MaterialTheme.typography.bodyMedium)
            Text(text = app.packageName, style = MaterialTheme.typography.bodySmall)
        }
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onAppSelected() }
        )
    }
}
