package ir.bedehyar.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ir.bedehyar.app.App
import ir.bedehyar.app.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fired by AlarmManager when a due-date reminder triggers.
 * Re-validates the transaction against the CURRENT database state (it may have
 * been settled or deleted since the alarm was set) and then starts the
 * foreground [AlarmService] which plays sound + vibration and posts the
 * full-screen notification.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION_FIRE) return
        val txId = intent.getLongExtra(AlarmActivity.EXTRA_ID, -1L)
        if (txId <= 0L) return

        val result = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.get(appContext)
                val settings = (appContext as App).settings.snapshot()

                val tw = db.txDao().getById(txId)
                if (tw == null || tw.isSettled || tw.tx.isArchived || !tw.tx.reminderEnabled) {
                    AlarmScheduler.cancel(appContext, txId)
                    db.reminderDao().deactivate(txId)
                    return@launch
                }
                if (!settings.remindersEnabled) {
                    // Master switch off -> stay silent, keep the reminder row for later.
                    return@launch
                }

                val service = Intent(appContext, AlarmService::class.java)
                    .putExtra(AlarmActivity.EXTRA_ID, txId)
                    .putExtra(AlarmActivity.EXTRA_NAME, tw.personName)
                    .putExtra(AlarmActivity.EXTRA_AMOUNT, tw.remaining)
                    .putExtra(AlarmActivity.EXTRA_IOWE, tw.tx.direction == ir.bedehyar.app.data.DIRECTION_I_OWE)
                    .putExtra(AlarmActivity.EXTRA_NOTE, tw.tx.description)
                    .putExtra(AlarmActivity.EXTRA_DUE, tw.tx.dueDate ?: 0L)

                androidx.core.content.ContextCompat.startForegroundService(appContext, service)
            } catch (_: Exception) {
                // Never crash the system broadcast; a missed ring is recoverable.
            } finally {
                result.finish()
            }
        }
    }
}
