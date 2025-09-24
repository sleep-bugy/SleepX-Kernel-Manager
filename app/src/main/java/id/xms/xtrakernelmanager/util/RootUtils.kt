package id.xms.xtrakernelmanager.util

import android.util.Log
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

object RootUtils {

    /**
     * @return
     */
    fun runCommandAsRoot(cmd: String, timeoutMs: Long = 3_000): String? = try {
        Log.d("RootUtils", "exec: $cmd")
        val proc = Runtime.getRuntime().exec("su")
        DataOutputStream(proc.outputStream).use { os ->
            os.writeBytes("$cmd\n")
            os.writeBytes("exit\n")
            os.flush()
        }

        // baca stdout & stderr secara paralel
        val out = proc.inputStream.bufferedReader().use { it.readText() }
        val err = proc.errorStream.bufferedReader().use { it.readText() }

        val ok = proc.waitFor(timeoutMs, TimeUnit.MILLISECONDS) && proc.exitValue() == 0
        if (!ok) {
            Log.e("RootUtils", "non-zero exit (${proc.exitValue()}) for: $cmd\nstderr=$err")
            null
        } else {
            Log.d("RootUtils", "output: $out")
            out.trim().takeIf { it.isNotEmpty() }
        }
    } catch (e: Exception) {
        Log.e("RootUtils", "exception: $cmd", e)
        null
    }

    /**
     * Mengecek apakah perangkat memiliki akses root.
     */
    fun isDeviceRooted(): Boolean {
        return runCommandAsRoot("id") != null
    }
}