package ir.bedehyar.app.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.app.NotificationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import ir.bedehyar.app.App
import ir.bedehyar.app.R
import ir.bedehyar.app.util.Jalali

/**
 * Foreground service that behaves like a phone alarm:
 * - plays a looping alarm sound on the ALARM stream (audible even in silent mode)
 * - raises the alarm volume to max while ringing
 * - vibrates in a repeating pattern
 * - posts a full-screen-intent notification on a high-importance channel
 */
class AlarmService : Service() {

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var prevAlarmVolume = -1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "bedehyar:alarmService")
        wakeLock?.acquire(15 * 60_000L)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            cleanupAndStop()
            return START_NOT_STICKY
        }
        val extras = intent?.extras ?: Bundle()
        startAsForeground(extras)
        startSound()
        startVibration()
        return START_NOT_STICKY
    }

    private fun startAsForeground(extras: Bundle) {
        val name = extras.getString(AlarmActivity.EXTRA_NAME) ?: ""
        val amount = extras.getLong(AlarmActivity.EXTRA_AMOUNT, 0L)
        val iOwe = extras.getBoolean(AlarmActivity.EXTRA_IOWE, true)
        val title = if (iOwe) "سررسید بدهی" else "سررسید طلب"
        val text = "$name — ${Jalali.price(amount)} تومان"

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

        val notif = NotificationCompat.Builder(this, App.CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPi, true)
            .setContentIntent(fullScreenPi)
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
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            prevAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            try {
                audioManager.setStreamVolume(
                    AudioManager.STREAM_ALARM,
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                    0
                )
            } catch (_: SecurityException) {
            }

            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            var ok = false
            try {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: Settings.System.DEFAULT_ALARM_ALERT_URI
                mp.setDataSource(this, uri)
                ok = true
            } catch (_: Exception) {
            }
            if (!ok) {
                mp.setDataSource(this, Uri.parse("android.resource://$packageName/${R.raw.alarm_tone}"))
            }
            mp.isLooping = true
            mp.prepare()
            mp.start()
            player = mp
        } catch (_: Exception) {
            player = null
        }
    }

    private fun startVibration() {
        val vib: Vibrator = if (Build.VERSION.SDK_INT >= 31) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vib.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 500), 0))
        vibrator = vib
    }

    private fun cleanupAndStop() {
        try {
            player?.stop()
            player?.release()
        } catch (_: Exception) {
        }
        player = null
        vibrator?.cancel()
        vibrator = null
        if (prevAlarmVolume >= 0) {
            try {
                (getSystemService(Context.AUDIO_SERVICE) as AudioManager)
                    .setStreamVolume(AudioManager.STREAM_ALARM, prevAlarmVolume, 0)
            } catch (_: Exception) {
            }
            prevAlarmVolume = -1
        }
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
        vibrator?.cancel()
        vibrator = null
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "ir.bedehyar.app.ACTION_STOP_ALARM"
        const val NOTIFICATION_ID = 4242

        fun stop(context: Context) {
            context.startService(
                Intent(context, AlarmService::class.java).setAction(ACTION_STOP)
            )
        }
    }
}
