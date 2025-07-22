package id.xms.xtrakernelmanager.util

import com.topjohnwu.superuser.Shell

object RootUtils {
    init {
        // Inisialisasi Shell dengan konfigurasi spesifik untuk KernelSU
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_MOUNT_MASTER or Shell.FLAG_NON_ROOT_SHELL)
                .setTimeout(10)
        )
    }

    fun isDeviceRooted(): Boolean {
        return try {
            // Cek izin root app dan coba eksekusi perintah sederhana
            val shell = Shell.getShell()
            shell.isRoot || shell.newJob().add("echo test").exec().isSuccess
        } catch (e: IllegalStateException) {
            // Fallback ke deteksi root dengan perintah "su" jika Shell.getShell() gagal
            return try {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "echo test"))
                val exitCode = process.waitFor()
                exitCode == 0
            } catch (ex: Exception) {
                false // Kembalikan false jika perintah "su" juga gagal
            }
        }
        catch (e: Exception) {
            false // Kembalikan false kalau ada error
        }
    }
}
