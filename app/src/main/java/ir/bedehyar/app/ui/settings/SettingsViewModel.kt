package ir.bedehyar.app.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.bedehyar.app.App
import ir.bedehyar.app.data.AppSettings
import ir.bedehyar.app.data.THEME_DARK
import ir.bedehyar.app.data.THEME_LIGHT
import ir.bedehyar.app.data.THEME_SYSTEM
import ir.bedehyar.app.util.BackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val appCtx = app
    val settings = (app as App).settings

    val busy = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)

    fun setTheme(mode: Int) = viewModelScope.launch { settings.setThemeMode(mode) }
    fun setRial(value: Boolean) = viewModelScope.launch { settings.setCurrencyRial(value) }
    fun setSound(value: Boolean) = viewModelScope.launch { settings.setAlarmSound(value) }
    fun setVibration(value: Boolean) = viewModelScope.launch { settings.setVibration(value) }
    fun setReminders(value: Boolean) = viewModelScope.launch { settings.setRemindersEnabled(value) }
    fun setSnooze(minutes: Int) = viewModelScope.launch { settings.setSnoozeMinutes(minutes) }
    fun setDefaultTime(hour: Int, minute: Int) =
        viewModelScope.launch { settings.setDefaultReminderTime(hour, minute) }

    suspend fun currentSettings(): AppSettings = settings.snapshot()

    fun exportBackup(uri: Uri) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            try {
                val json = BackupManager.exportJson(appCtx)
                withContext(Dispatchers.IO) {
                    appCtx.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(json.toByteArray(Charsets.UTF_8))
                        os.flush()
                    } ?: error("نمی‌توان فایل را ایجاد کرد")
                }
                message.value = "نسخه پشتیبان ذخیره شد"
            } catch (e: Exception) {
                message.value = "خطا در پشتیبان‌گیری: ${e.message}"
            } finally {
                busy.value = false
            }
        }
    }

    /** Reads a backup file and returns validation summary text (Persian) or error. */
    suspend fun previewBackup(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val text = appCtx.contentResolver.openInputStream(uri)?.use { ins ->
                ins.readBytes().toString(Charsets.UTF_8)
            } ?: return@withContext "فایل خوانده نشد"
            val root = org.json.JSONObject(text)
            if (root.optString("app") != "hesabyar") return@withContext "فایل پشتیبان معتبر نیست"
            val v = root.optInt("formatVersion", -1)
            if (v !in 1..BackupManager.FORMAT_VERSION) return@withContext "نسخه فایل پشتیبان پشتیبانی نمی‌شود"
            val persons = root.getJSONArray("persons").length()
            val txs = root.getJSONArray("transactions").length()
            val pays = root.optJSONArray("payments")?.length() ?: 0
            "این فایل شامل ${persons} شخص، $txs تراکنش و $pays پرداخت است.\n" +
                "تمام اطلاعات فعلی جایگزین می‌شود. ادامه می‌دهید؟"
        } catch (e: Exception) {
            "فایل پشتیبان معتبر نیست: ${e.message}"
        }
    }

    fun importBackup(uri: Uri) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    appCtx.contentResolver.openInputStream(uri)?.use { ins ->
                        ins.readBytes().toString(Charsets.UTF_8)
                    } ?: error("فایل خوانده نشد")
                }
                val summary = BackupManager.importJson(appCtx, text)
                message.value = "بازیابی انجام شد: ${summary.persons} شخص، ${summary.transactions} تراکنش"
            } catch (e: Exception) {
                message.value = "بازیابی ناموفق — داده‌ها دست‌نخورده ماندند: ${e.message}"
            } finally {
                busy.value = false
            }
        }
    }

    fun consumeMessage() { message.value = null }
}
