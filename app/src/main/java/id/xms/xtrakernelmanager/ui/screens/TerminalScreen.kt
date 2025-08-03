package id.xms.xtrakernelmanager.ui.screens

// Import BackHandler DIHAPUS
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear // Untuk tombol clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import id.xms.xtrakernelmanager.viewmodel.TerminalViewModel
import kotlinx.coroutines.launch


@Composable
fun TerminalScreen(
    vm: TerminalViewModel = hiltViewModel(),
    navController: NavController? = null
) {
    val rawOutput by vm.output.collectAsState()
    val outputLines = remember(rawOutput) { rawOutput.lines().filter { it.isNotBlank() } }

    var currentCommand by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val terminalTextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp,
        color = Color.White
    )
    val prompt = "~"

    val executeCommand = { commandToExecute: String ->
        if (commandToExecute.isNotBlank()) {
            vm.exec(commandToExecute)
            currentCommand = ""
            coroutineScope.launch {
                kotlinx.coroutines.delay(100)
                val targetIndex = if (outputLines.isNotEmpty()) outputLines.size else 0
                lazyListState.animateScrollToItem(targetIndex.coerceAtLeast(0))
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    LaunchedEffect(outputLines.size) {
        val targetIndex = if (outputLines.isNotEmpty()) outputLines.size else 0
        if (lazyListState.layoutInfo.totalItemsCount > 0 && targetIndex < lazyListState.layoutInfo.totalItemsCount) {
            lazyListState.animateScrollToItem(targetIndex.coerceAtLeast(0))
        } else if (lazyListState.layoutInfo.totalItemsCount > 0) {
            lazyListState.animateScrollToItem(
                (lazyListState.layoutInfo.totalItemsCount - 1).coerceAtLeast(
                    0
                )
            )
        }
    }

    Scaffold(
        containerColor = Color.Black
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.weight(1f)
            ) {
                items(outputLines) { line ->
                    Text(
                        text = line,
                        style = terminalTextStyle,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item { // Baris input
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$prompt ",
                            style = terminalTextStyle.copy(color = Color(0xFF4CAF50))
                        )
                        BasicTextField(
                            value = currentCommand,
                            onValueChange = { currentCommand = it },
                            textStyle = terminalTextStyle.copy(color = Color.White),
                            cursorBrush = SolidColor(Color.White),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = { executeCommand(currentCommand) }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                        )
                        if (currentCommand.isNotBlank()) {
                            IconButton(
                                onClick = { currentCommand = "" },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Clear command",
                                    tint = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }


                Row( // Tombol Send eksternal
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = { executeCommand(currentCommand) },
                        enabled = currentCommand.isNotBlank()
                    ) {
                        Icon(
                            Icons.Filled.ArrowForward,
                            contentDescription = "Send Command",
                            tint = Color.White
                        )
                    }
                }
            }
        }
}
