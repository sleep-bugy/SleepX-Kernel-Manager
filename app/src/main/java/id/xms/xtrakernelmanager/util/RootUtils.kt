package id.xms.xtrakernelmanager.util

import android.util.Log
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object RootUtils {

    // Cache for root status to avoid repeated checks
    private var rootStatusCache: Boolean? = null
    private var lastRootCheck = 0L
    private const val ROOT_CHECK_CACHE_TIME = 10_000L // 10 seconds cache

    // Mutex to prevent concurrent root operations
    private val rootMutex = Mutex()

    // Initialize Shell instance with proper configuration
    init {
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_MOUNT_MASTER)
                .setTimeout(3)
        )
    }

    /**
     * Execute command as root using libsu for better performance (synchronous)
     */
    fun runCommandAsRoot(cmd: String, timeoutMs: Long = 2_000): String? {
        return try {
            Log.d("RootUtils", "exec: $cmd")

            if (!isDeviceRooted()) {
                Log.e("RootUtils", "Root access not granted")
                return null
            }

            val result = Shell.cmd(cmd).exec()

            if (result.isSuccess) {
                val output = result.out.joinToString("\n").trim()
                Log.d("RootUtils", "output: $output")
                output.takeIf { it.isNotEmpty() }
            } else {
                Log.e("RootUtils", "Command failed: $cmd, code: ${result.code}")
                Log.e("RootUtils", "stderr: ${result.err.joinToString("\n")}")
                null
            }
        } catch (e: Exception) {
            Log.e("RootUtils", "Exception executing command: $cmd", e)
            null
        }
    }

    /**
     * Asynchronous version for use in coroutines with mutex to prevent concurrent operations
     */
    suspend fun runCommandAsRootAsync(cmd: String): String? = withContext(Dispatchers.IO) {
        rootMutex.withLock {
            runCommandAsRoot(cmd)
        }
    }

    /**
     * Check if device has root access using libsu with caching
     */
    fun isDeviceRooted(): Boolean {
        val now = System.currentTimeMillis()

        // Use cached result if available and not expired
        rootStatusCache?.let { cachedStatus ->
            if (now - lastRootCheck < ROOT_CHECK_CACHE_TIME) {
                Log.d("RootUtils", "Using cached root status: $cachedStatus")
                return cachedStatus
            }
        }

        return try {
            // Try multiple methods to detect root
            val hasShellAccess = Shell.isAppGrantedRoot() == true
            val hasRootBinary = checkRootBinary()
            val hasSuAccess = checkSuAccess()

            val isRooted = hasShellAccess || hasRootBinary || hasSuAccess

            // Cache the result
            rootStatusCache = isRooted
            lastRootCheck = now

            Log.d(
                "RootUtils",
                "Root check result - Shell: $hasShellAccess, Binary: $hasRootBinary, Su: $hasSuAccess, Final: $isRooted"
            )
            isRooted
        } catch (e: Exception) {
            Log.e("RootUtils", "Error checking root status", e)
            // Don't cache error results
            false
        }
    }

    /**
     * Check if root binary exists
     */
    private fun checkRootBinary(): Boolean {
        return try {
            val locations = arrayOf(
                "/system/bin/su",
                "/system/xbin/su",
                "/sbin/su",
                "/vendor/bin/su"
            )
            locations.any { location ->
                try {
                    java.io.File(location).exists()
                } catch (e: Exception) {
                    false
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if su command is accessible
     */
    private fun checkSuAccess(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su --version")
            val result = process.waitFor()
            result == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Force refresh root status cache
     */
    fun refreshRootStatus() {
        rootStatusCache = null
        lastRootCheck = 0L
    }

    /**
     * Legacy method for compatibility - now uses libsu
     */
    fun execute(command: String): String? {
        return runCommandAsRoot(command)
    }

    /**
     * Execute multiple commands in a single shell session for better performance
     */
    fun runCommandsAsRoot(commands: List<String>): List<String> {
        return try {
            if (!isDeviceRooted()) {
                Log.e("RootUtils", "Root access not granted")
                return emptyList()
            }

            val result = Shell.cmd(*commands.toTypedArray()).exec()

            if (result.isSuccess) {
                result.out
            } else {
                Log.e("RootUtils", "Commands failed, code: ${result.code}")
                Log.e("RootUtils", "stderr: ${result.err.joinToString("\n")}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("RootUtils", "Exception executing commands", e)
            emptyList()
        }
    }

    /**
     * Check if a specific command/binary exists
     */
    fun commandExists(command: String): Boolean {
        return try {
            val result = Shell.cmd("which $command").exec()
            result.isSuccess && result.out.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get shell version info for debugging
     */
    fun getShellInfo(): String {
        return try {
            val shell = Shell.getShell()
            "Shell: ${shell.javaClass.simpleName}, Status: ${shell.status}"
        } catch (e: Exception) {
            "Shell info unavailable: ${e.message}"
        }
    }
}