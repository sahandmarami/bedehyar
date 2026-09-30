package ir.bedehyar.app.alarm

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION_FIRE) return
        val extras = intent.extras ?: return
        if (!extras.containsKey(AlarmActivity.EXTRA_ID)) return

        // 1) Start the foreground service that plays the alarm sound + vibration.
        val svc = Intent(context, AlarmService::class.java).putExtras(Bundle(extras))
        context.startForegroundService(svc)

        // 2) Try to open the full-screen AlarmActivity directly.
        //    On Android 10+ background activity starts are blocked unless the app
        //    can draw overlays or holds an exact-alarm permission (exempt on 14+).
        val canDraw = Settings.canDrawOverlays(context)
        val canExact = if (Build.VERSION.SDK_INT >= 31) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            try { am.canScheduleExactAlarms() } catch (_: Exception) { false }
        } else {
            true
        }
        if (Build.VERSION.SDK_INT < 29 || canDraw || canExact) {
            try {
                context.startActivity(
                    Intent(context, AlarmActivity::class.java)
                        .addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                        )
                        .putExtras(Bundle(extras))
                )
            } catch (_: Exception) {
            }
        }
        // 3) The service also posts a full-screen-intent notification
        //    (channel IMPORTANCE_HIGH) which opens AlarmActivity on lock screen.
    }
}
