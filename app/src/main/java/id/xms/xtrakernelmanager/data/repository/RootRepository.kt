package id.xms.xtrakernelmanager.data.repository

import com.topjohnwu.superuser.Shell
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RootRepository @Inject constructor() {
    fun isRooted(): Boolean = Shell.getShell().isRoot
    fun run(cmd: String): String = Shell.cmd(cmd).exec().out.joinToString("\n")
}