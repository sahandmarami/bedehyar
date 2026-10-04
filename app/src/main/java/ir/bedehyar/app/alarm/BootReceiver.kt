package ir.bedehyar.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ir.bedehyar.app.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * After a reboot or an app update every scheduled alarm is gone (alarms do not
 * survive reboots), so this receiver rebuilds them from the local `reminders`
 * table. Rows in the past are deactivated instead of re-fired, which prevents
 * a flood of stale alarms.
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
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = appContext as App
                val master = app.settings.snapshot().remindersEnabled
                AlarmScheduler.rebuildAll(appContext, master)
            } catch (_: Exception) {
            } finally {
                result.finish()
            }
        }
    }
}
