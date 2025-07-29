package id.xms.xtrakernelmanager.ui.components

import android.graphics.drawable.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import org.w3c.dom.Text

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppToolbar(title: String, onBack: (() -> Unit)? = null) {
    if (onBack != null) {
        TopAppBar(title = { Text(title) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "back")
                }
            })
    } else {
        CenterAlignedTopAppBar(title = { Text(title) })
    }
}