package id.xms.xtrakernelmanager.util

import android.content.Context
import android.widget.Toast
import id.xms.xtrakernelmanager.data.repository.RootRepository
import javax.inject.Inject

class RootUtils @Inject constructor(
    private val rootRepo: RootRepository
) {
    fun init(ctx: Context) {
        if (!rootRepo.isRooted()) {
            Toast.makeText(ctx, "Root access required", Toast.LENGTH_LONG).show()
        }
    }
}