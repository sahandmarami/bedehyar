package ir.bedehyar.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ir.bedehyar.app.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-schedules all pending alarms after a device reboot or app update.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (
            action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON" &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.get(context).dao()
                val now = System.currentTimeMillis()
                dao.pendingAlarms(now).forEach { AlarmScheduler.schedule(context, it) }
            } catch (_: Exception) {
            } finally {
                result.finish()
            }
        }
    }
}
