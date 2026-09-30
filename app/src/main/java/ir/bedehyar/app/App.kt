package ir.bedehyar.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        createChannels()
    }

    private fun createChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        val alarmChannel = NotificationChannel(
            CHANNEL_ALARM,
            "آلارم سررسید",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "اعلان آلارم هنگام رسیدن سررسید بدهی و طلب"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 700, 500)
            setSound(null, null)
            setBypassDnd(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(alarmChannel)
    }

    companion object {
        const val CHANNEL_ALARM = "alarm_channel"
    }
}
