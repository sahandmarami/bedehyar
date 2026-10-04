package ir.bedehyar.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import ir.bedehyar.app.MainActivity
import ir.bedehyar.app.data.AppDatabase
import ir.bedehyar.app.data.DebtTransaction

/**
 * Exact due-date scheduling built on AlarmManager.
 *
 * - Uses setAlarmClock() (the "user alarm clock" API) whenever exact alarms are
 *   allowed; it survives Doze and shows the upcoming alarm in the system UI.
 * - On Android 12+ exact alarms need SCHEDULE_EXACT_ALARM; when the user has not
 *   granted it we degrade gracefully to setWindow() (±10 minutes) and the
 *   Settings screen offers a one-tap shortcut to grant the permission.
 * - Every alarm is mirrored into the `reminders` table so it can be rebuilt
 *   after reboot or app update (see BootReceiver).
 * - Snoozing schedules a one-shot alarm N minutes ahead WITHOUT touching the
 *   original due date.
 * - Past due dates are never (re)scheduled — this prevents endless alarm loops.
 */
object AlarmScheduler {

    // Kept identical to v1 so pending alarms of the previous release still resolve.
    const val ACTION_FIRE = "ir.bedehyar.app.ACTION_FIRE_ALARM"

    fun fireIntent(context: Context, txId: Long): Intent =
        Intent(context, AlarmReceiver::class.java)
            .setAction(ACTION_FIRE)
            .putExtra(AlarmActivity.EXTRA_ID, txId)

    private fun firePendingIntent(context: Context, txId: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            txId.toInt(),
            fireIntent(context, txId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun showIntent(context: Context, txId: Long): PendingIntent =
        PendingIntent.getActivity(
            context,
            txId.toInt(),
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(MainActivity.EXTRA_ROUTE, "tx/$txId"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /** Computes the wall-clock trigger for a due date + reminder time (device timezone). */
    fun triggerAtMillis(dueDate: Long, hour: Int, minute: Int): Long {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = dueDate
        cal.set(java.util.Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
        cal.set(java.util.Calendar.MINUTE, minute.coerceIn(0, 59))
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * Schedules the reminder of [tx] at its due date + reminder time.
     * Returns the trigger millis, or null when nothing was scheduled.
     */
    fun schedule(context: Context, tx: DebtTransaction, masterEnabled: Boolean): Long? {
        cancel(context, tx.id)
        if (!masterEnabled || !tx.reminderEnabled || tx.dueDate == null) return null
        val trigger = triggerAtMillis(tx.dueDate, tx.reminderHour, tx.reminderMinute)
        if (trigger <= System.currentTimeMillis()) return null // never loop on past dues
        return scheduleAt(context, tx.id, trigger)
    }

    /** Schedules a one-shot alarm at an absolute time (also used for snooze/restore). */
    fun scheduleAt(context: Context, txId: Long, triggerAt: Long): Long? {
        if (triggerAt <= System.currentTimeMillis()) return null
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val op = firePendingIntent(context, txId)
        val canExact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        try {
            if (canExact) {
                am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showIntent(context, txId)), op)
            } else {
                am.setWindow(AlarmManager.RTC_WAKEUP, triggerAt, 10 * 60_000L, op)
            }
        } catch (_: SecurityException) {
            try {
                am.setWindow(AlarmManager.RTC_WAKEUP, triggerAt, 10 * 60_000L, op)
            } catch (_: Exception) {
                return null
            }
        }
        return triggerAt
    }

    /** Cancels the pending alarm (reminder rows are left to the caller). */
    fun cancel(context: Context, txId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(firePendingIntent(context, txId))
    }

    /** Schedules a snooze alarm N minutes from now; original due date is untouched. */
    suspend fun snooze(context: Context, txId: Long, minutes: Int): Boolean {
        val db = AppDatabase.get(context)
        val trigger = System.currentTimeMillis() + minutes.coerceIn(1, 720) * 60_000L
        val scheduled = scheduleAt(context, txId, trigger)
        if (scheduled != null) {
            db.reminderDao().upsert(
                ir.bedehyar.app.data.Reminder(transactionId = txId, triggerAt = scheduled, isActive = true)
            )
        }
        return scheduled != null
    }

    /**
     * Rebuilds alarms from the `reminders` table (boot / app update / restore).
     * Rows whose trigger is in the past are deactivated, not re-fired.
     */
    suspend fun rebuildAll(context: Context, masterEnabled: Boolean) {
        val db = AppDatabase.get(context)
        val now = System.currentTimeMillis()
        db.reminderDao().allActive(now).forEach { reminder ->
            val tw = db.txDao().getById(reminder.transactionId)
            if (tw == null || tw.isSettled || tw.tx.isArchived || !tw.tx.reminderEnabled || !masterEnabled) {
                db.reminderDao().deactivate(reminder.transactionId)
            } else {
                scheduleAt(context, reminder.transactionId, reminder.triggerAt)
            }
        }
    }

    /** Cancels every pending alarm (used by restore/replace-data). */
    suspend fun cancelAll(context: Context, db: AppDatabase) {
        val now = System.currentTimeMillis()
        db.reminderDao().allActive(now).forEach { cancel(context, it.transactionId) }
        // Legacy v1 rows may have pending alarms without reminder rows; cancel by tx ids too.
        db.txDao().queryTransactions(0L, 0L, 0L, 0L, -1).forEach { cancel(context, it.tx.id) }
    }
}
