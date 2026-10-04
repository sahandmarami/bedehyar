package ir.bedehyar.app.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import ir.bedehyar.app.App
import ir.bedehyar.app.MainActivity
import ir.bedehyar.app.R
import ir.bedehyar.app.util.Jalali
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Foreground service that rings a due-date reminder:
 *  - plays the bundled alarm tone in a loop on the ALARM stream (USAGE_ALARM),
 *    therefore it always follows the user's alarm-volume settings; it never
 *    changes or boosts system volumes.
 *  - vibrates in a repeating pattern (both stop with the user's action)
 *  - posts a full-screen-intent notification on a high-importance channel
 *  - stops completely (sound + vibration + notification) on «خاموش کردن»
 *    and schedules a one-shot snooze alarm on «۱۰ دقیقه بعد».
 */
class AlarmService : Service() {

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentTxId = -1L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "bedehyar:alarmService")
        wakeLock?.acquire(15 * 60_000L)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                cleanupAndStop()
                return START_NOT_STICKY
            }
            ACTION_SNOOZE -> {
                val txId = intent.getLongExtra(AlarmActivity.EXTRA_ID, currentTxId)
                doSnooze(txId)
                return START_NOT_STICKY
            }
        }
        val extras = intent?.extras ?: Bundle()
        currentTxId = extras.getLong(AlarmActivity.EXTRA_ID, -1L)

        // Foreground must be entered promptly; ringing starts right after.
        startAsForeground(extras)
        val app = applicationContext as App
        CoroutineScope(Dispatchers.Main).launch {
            val s = app.settings.snapshot()
            if (s.alarmSound) startSound() else player = null
            if (s.vibration) startVibration()
        }
        return START_NOT_STICKY
    }

    private fun doSnooze(txId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = applicationContext as App
                val s = app.settings.snapshot()
                if (txId > 0) AlarmScheduler.snooze(applicationContext, txId, s.snoozeMinutes)
            } catch (_: Exception) {
            } finally {
                kotlinx.coroutines.withContext(Dispatchers.Main) { cleanupAndStop() }
            }
        }
    }

    private fun startAsForeground(extras: Bundle) {
        val name = extras.getString(AlarmActivity.EXTRA_NAME) ?: ""
        val amount = extras.getLong(AlarmActivity.EXTRA_AMOUNT, 0L)
        val iOwe = extras.getBoolean(AlarmActivity.EXTRA_IOWE, true)
        val title = "زمان سررسید رسیده است"
        val typeLabel = if (iOwe) "بدهی من" else "طلب من از او"
        val text = "$name — $typeLabel — ${Jalali.price(amount)} ${Jalali.currencySuffix()}"

        val fullScreenPi = PendingIntent.getActivity(
            this,
            9999,
            Intent(this, AlarmActivity::class.java)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
                .putExtras(Bundle(extras)),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopPi = PendingIntent.getService(
            this, 1,
            Intent(this, AlarmService::class.java).setAction(ACTION_STOP)
                .putExtra(AlarmActivity.EXTRA_ID, currentTxId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snoozePi = PendingIntent.getService(
            this, 2,
            Intent(this, AlarmService::class.java).setAction(ACTION_SNOOZE)
                .putExtra(AlarmActivity.EXTRA_ID, currentTxId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(this, App.CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPi, true)
            .setContentIntent(fullScreenPi)
            .addAction(R.drawable.ic_stat_alarm, "خاموش کردن", stopPi)
            .addAction(R.drawable.ic_stat_alarm, "۱۰ دقیقه بعد", snoozePi)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notif)
        }
    }

    private fun startSound() {
        if (player != null) return
        try {
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            var ok = false
            try {
                val afd = resources.openRawResourceFd(R.raw.alarm_tone)
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                ok = true
            } catch (_: Exception) {
            }
            if (!ok) {
                val uri: Uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                    ?: android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI
                mp.setDataSource(this, uri)
            }
            mp.isLooping = true
            mp.prepare()
            mp.start()
            player = mp
        } catch (_: Exception) {
            try { player?.release() } catch (_: Exception) {}
            player = null
        }
    }

    private fun startVibration() {
        try {
            val vib: Vibrator = if (Build.VERSION.SDK_INT >= 31) {
                (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vib.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 500), 0))
            vibrator = vib
        } catch (_: Exception) {
            vibrator = null
        }
    }

    private fun cleanupAndStop() {
        try {
            player?.stop()
            player?.release()
        } catch (_: Exception) {
        }
        player = null
        try { vibrator?.cancel() } catch (_: Exception) {}
        vibrator = null
        if (Build.VERSION.SDK_INT >= 24) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        try {
            player?.stop()
            player?.release()
        } catch (_: Exception) {
        }
        player = null
        try { vibrator?.cancel() } catch (_: Exception) {}
        vibrator = null
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "ir.bedehyar.app.ACTION_STOP_ALARM"
        const val ACTION_SNOOZE = "ir.bedehyar.app.ACTION_SNOOZE_ALARM"
        const val NOTIFICATION_ID = 4242

        fun stop(context: Context) {
            try {
                context.startService(
                    Intent(context, AlarmService::class.java).setAction(ACTION_STOP)
                )
            } catch (_: Exception) {
            }
        }

        fun snooze(context: Context, txId: Long) {
            try {
                context.startService(
                    Intent(context, AlarmService::class.java)
                        .setAction(ACTION_SNOOZE)
                        .putExtra(AlarmActivity.EXTRA_ID, txId)
                )
            } catch (_: Exception) {
            }
        }
    }
}
