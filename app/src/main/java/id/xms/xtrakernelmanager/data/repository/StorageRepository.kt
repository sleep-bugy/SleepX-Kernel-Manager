package id.xms.xtrakernelmanager.data.repository

import android.content.Context
import android.os.Environment
import android.os.StatFs
import dagger.hilt.android.qualifiers.ApplicationContext
import id.xms.xtrakernelmanager.data.model.StorageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun getStorageInfo(): Flow<StorageInfo> = flow {
        try {
            // Gunakan path storage yang paling akurat untuk Android modern
            // Environment.getExternalStorageDirectory() menunjuk ke internal storage pada perangkat modern
            val internalStorageDir = Environment.getExternalStorageDirectory()
            val internalStatFs = StatFs(internalStorageDir.path)

            val blockSize = internalStatFs.blockSizeLong
            val totalBlocks = internalStatFs.blockCountLong
            val availableBlocks = internalStatFs.availableBlocksLong

            val internalTotalSpace = totalBlocks * blockSize
            val internalFreeSpace = availableBlocks * blockSize
            val internalUsedSpace = internalTotalSpace - internalFreeSpace

            // Cek apakah ada SD card eksternal yang benar-benar terpisah
            var externalTotalSpace: Long? = null
            var externalFreeSpace: Long? = null
            var externalUsedSpace: Long? = null
            var hasExternalStorage = false

            try {
                // Cek external files directories untuk SD card
                val externalDirs = context.getExternalFilesDirs(null)
                for (dir in externalDirs) {
                    if (dir != null && Environment.isExternalStorageRemovable(dir)) {
                        // Pastikan ini bukan internal storage yang sama
                        if (!dir.absolutePath.startsWith(internalStorageDir.absolutePath)) {
                            try {
                                val externalStatFs = StatFs(dir.path)
                                val extBlockSize = externalStatFs.blockSizeLong
                                val extTotalBlocks = externalStatFs.blockCountLong
                                val extAvailableBlocks = externalStatFs.availableBlocksLong

                                externalTotalSpace = extTotalBlocks * extBlockSize
                                externalFreeSpace = extAvailableBlocks * extBlockSize
                                externalUsedSpace = externalTotalSpace - externalFreeSpace
                                hasExternalStorage = true
                                break
                            } catch (e: Exception) {
                                // Ignore dan coba yang berikutnya
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                hasExternalStorage = false
            }

            // Total space hanya internal storage (jangan tambahkan external)
            val totalSpace = internalTotalSpace
            val freeSpace = internalFreeSpace
            val usedSpace = internalUsedSpace

            val storageInfo = StorageInfo(
                totalSpace = totalSpace,
                freeSpace = freeSpace,
                usedSpace = usedSpace,
                internalTotalSpace = internalTotalSpace,
                internalFreeSpace = internalFreeSpace,
                internalUsedSpace = internalUsedSpace,
                externalTotalSpace = externalTotalSpace,
                externalFreeSpace = externalFreeSpace,
                externalUsedSpace = externalUsedSpace,
                hasExternalStorage = hasExternalStorage
            )

            emit(storageInfo)

        } catch (e: Exception) {
            // Fallback sederhana
            emit(StorageInfo(
                totalSpace = 0L,
                freeSpace = 0L,
                usedSpace = 0L,
                internalTotalSpace = 0L,
                internalFreeSpace = 0L,
                internalUsedSpace = 0L,
                externalTotalSpace = null,
                externalFreeSpace = null,
                externalUsedSpace = null,
                hasExternalStorage = false
            ))
        }
    }.flowOn(Dispatchers.IO)

    // Helper function to format storage size
    fun formatStorageSize(bytes: Long): String {
        val tb = 1024L * 1024L * 1024L * 1024L
        val gb = 1024L * 1024L * 1024L
        val mb = 1024L * 1024L
        val kb = 1024L

        return when {
            bytes >= tb -> String.format(Locale.getDefault(), "%.1f TB", bytes.toDouble() / tb)
            bytes >= gb -> String.format(Locale.getDefault(), "%.1f GB", bytes.toDouble() / gb)
            bytes >= mb -> String.format(Locale.getDefault(), "%.1f MB", bytes.toDouble() / mb)
            bytes >= kb -> String.format(Locale.getDefault(), "%.1f KB", bytes.toDouble() / kb)
            else -> "$bytes B"
        }
    }
}
