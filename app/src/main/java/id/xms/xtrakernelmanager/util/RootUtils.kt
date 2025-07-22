package id.xms.xtrakernelmanager.util

import java.io.DataOutputStream
import java.io.IOException

object RootUtils {
    fun isDeviceRooted(): Boolean {
        return try {
            // Cek dengan menjalankan perintah su
            val process = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(process.outputStream)
            outputStream.writeBytes("exit\n")
            outputStream.flush()
            val exitValue = process.waitFor()
            exitValue == 0 // Exit value 0 berarti su berhasil dijalankan
        } catch (e: IOException) {
            false // Error berarti non-root
        } catch (e: InterruptedException) {
            false // Error berarti non-root
        } catch (e: SecurityException) {
            false // Error berarti non-root
        }
    }
}