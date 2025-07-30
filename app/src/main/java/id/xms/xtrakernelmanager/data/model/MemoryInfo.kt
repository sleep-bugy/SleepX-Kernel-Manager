package id.xms.xtrakernelmanager.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class MemoryInfo(
    val used: Long,
    val total: Long,
    val free: Long,
)