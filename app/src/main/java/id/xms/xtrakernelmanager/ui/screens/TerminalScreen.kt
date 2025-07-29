package id.xms.xtrakernelmanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import id.xms.xtrakernelmanager.ui.components.TerminalShortcutCard
import id.xms.xtrakernelmanager.viewmodel.TerminalViewModel

@Composable
fun TerminalScreen(vm: TerminalViewModel = hiltViewModel()) {
    val output by vm.output.collectAsState()
    var cmd by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            reverseLayout = true
        ) {
            items(output.lines().size) { idx ->
                Text(
                    text = output.lines()[idx],
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        TerminalShortcutCard { vm.exec(it) }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = cmd,
                onValueChange = { cmd = it },
                label = { Text("Command") },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                vm.exec(cmd)
                cmd = ""
            }) {
                Icon(Icons.Default.Send, contentDescription = "send")
            }
        }
    }
}