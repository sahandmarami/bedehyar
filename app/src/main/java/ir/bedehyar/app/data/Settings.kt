package ir.bedehyar.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Theme modes. */
const val THEME_SYSTEM = 0
const val THEME_LIGHT = 1
const val THEME_DARK = 2

data class AppSettings(
    val themeMode: Int = THEME_SYSTEM,
    val currencyRial: Boolean = false,          // false = تومان، true = ریال
    val alarmSound: Boolean = true,
    val vibration: Boolean = true,
    val remindersEnabled: Boolean = true,       // کلید اصلی یادآوری‌ها
    val snoozeMinutes: Int = 10,
    val defaultReminderHour: Int = 9,
    val defaultReminderMinute: Int = 0,
    val guideShown: Boolean = false
)

/**
 * Persistent app settings backed by Jetpack DataStore.
 * Nothing here ever leaves the device.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = intPreferencesKey("theme_mode")
        val RIAL = booleanPreferencesKey("currency_rial")
        val SOUND = booleanPreferencesKey("alarm_sound")
        val VIBRATION = booleanPreferencesKey("alarm_vibration")
        val REMINDERS = booleanPreferencesKey("reminders_enabled")
        val SNOOZE = intPreferencesKey("snooze_minutes")
        val DEF_HOUR = intPreferencesKey("default_reminder_hour")
        val DEF_MINUTE = intPreferencesKey("default_reminder_minute")
        val GUIDE_SHOWN = booleanPreferencesKey("guide_shown")
    }

    val flow: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            themeMode = p[Keys.THEME] ?: THEME_SYSTEM,
            currencyRial = p[Keys.RIAL] ?: false,
            alarmSound = p[Keys.SOUND] ?: true,
            vibration = p[Keys.VIBRATION] ?: true,
            remindersEnabled = p[Keys.REMINDERS] ?: true,
            snoozeMinutes = p[Keys.SNOOZE] ?: 10,
            defaultReminderHour = p[Keys.DEF_HOUR] ?: 9,
            defaultReminderMinute = p[Keys.DEF_MINUTE] ?: 0,
            guideShown = p[Keys.GUIDE_SHOWN] ?: false
        )
    }

    suspend fun snapshot(): AppSettings = flow.first()

    suspend fun setThemeMode(value: Int) = context.dataStore.edit { it[Keys.THEME] = value }
    suspend fun setCurrencyRial(value: Boolean) = context.dataStore.edit { it[Keys.RIAL] = value }
    suspend fun setAlarmSound(value: Boolean) = context.dataStore.edit { it[Keys.SOUND] = value }
    suspend fun setVibration(value: Boolean) = context.dataStore.edit { it[Keys.VIBRATION] = value }
    suspend fun setRemindersEnabled(value: Boolean) = context.dataStore.edit { it[Keys.REMINDERS] = value }
    suspend fun setSnoozeMinutes(value: Int) = context.dataStore.edit { it[Keys.SNOOZE] = value }
    suspend fun setDefaultReminderTime(hour: Int, minute: Int) = context.dataStore.edit {
        it[Keys.DEF_HOUR] = hour
        it[Keys.DEF_MINUTE] = minute
    }

    suspend fun setGuideShown(value: Boolean) = context.dataStore.edit { it[Keys.GUIDE_SHOWN] = value }
}
