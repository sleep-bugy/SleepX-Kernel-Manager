package id.xms.xtrakernelmanager.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object EasterEggUtils {
    var clickCount by mutableStateOf(0)
    const val MAX_CLICKS = 5
    var isEggTriggered by mutableStateOf(false)

    fun resetClickCount() {
        clickCount = 0
        isEggTriggered = false
    }

    fun onClick(): Boolean {
        clickCount++
        if (clickCount >= MAX_CLICKS) {
            isEggTriggered = true
            clickCount = 0
            return true
        }
        return false
    }
}