package ir.bedehyar.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import ir.bedehyar.app.data.AppDatabase
import ir.bedehyar.app.data.Ledger
import ir.bedehyar.app.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class App : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val settings: SettingsRepository by lazy { SettingsRepository(this) }
    val ledger: Ledger by lazy { Ledger(this, AppDatabase.get(this), settings) }

    override fun onCreate() {
        super.onCreate()
        createChannels()
        appScope.launch { ledger.ensureCategories() }
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
            setSound(null, null) // sound is played by AlarmService itself (no double ring)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(alarmChannel)
    }

    companion object {
        const val CHANNEL_ALARM = "alarm_channel"
    }
}
