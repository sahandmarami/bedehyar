package ir.bedehyar.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import ir.bedehyar.app.MainActivity
import ir.bedehyar.app.data.DebtTransaction

/**
 * Schedules exact alarms with AlarmManager.setAlarmClock().
 * setAlarmClock() works on every version from API 21+, bypasses Doze and
 * does not require the SCHEDULE_EXACT_ALARM permission (it is the "alarm clock" API).
 */
object AlarmScheduler {

    const val ACTION_FIRE = "ir.bedehyar.app.ACTION_FIRE_ALARM"

    fun fireIntent(context: Context, tx: DebtTransaction): Intent =
        Intent(context, AlarmReceiver::class.java)
            .setAction(ACTION_FIRE)
            .putExtra(AlarmActivity.EXTRA_ID, tx.id)
            .putExtra(AlarmActivity.EXTRA_NAME, tx.personName)
            .putExtra(AlarmActivity.EXTRA_AMOUNT, tx.remaining)
            .putExtra(AlarmActivity.EXTRA_IOWE, tx.iOwe)
            .putExtra(AlarmActivity.EXTRA_NOTE, tx.note)

    private fun firePendingIntent(context: Context, tx: DebtTransaction): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            tx.id.toInt(),
            fireIntent(context, tx),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun schedule(context: Context, tx: DebtTransaction) {
        if (!tx.alarmEnabled || tx.dueAt == null || tx.isSettled) return
        if (tx.dueAt <= System.currentTimeMillis()) return
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val showIntent = PendingIntent.getActivity(
            context,
            tx.id.toInt(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val info = AlarmManager.AlarmClockInfo(tx.dueAt, showIntent)
        try {
            am.setAlarmClock(info, firePendingIntent(context, tx))
        } catch (_: SecurityException) {
        }
    }

    fun cancel(context: Context, id: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(
            PendingIntent.getBroadcast(
                context,
                id.toInt(),
                Intent(context, AlarmReceiver::class.java).setAction(ACTION_FIRE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    }
}
