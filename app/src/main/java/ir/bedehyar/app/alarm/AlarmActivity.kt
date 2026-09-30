package ir.bedehyar.app.alarm

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ir.bedehyar.app.data.AppDatabase
import ir.bedehyar.app.ui.theme.BedehyarTheme
import kotlinx.coroutines.runBlocking

/**
 * Full-screen alarm page. Opens automatically over any app and over the lock screen.
 */
class AlarmActivity : ComponentActivity() {

    data class Payload(
        val id: Long,
        val name: String,
        val amount: Long,
        val iOwe: Boolean,
        val note: String
    )

    private var payload by mutableStateOf<Payload?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyLockScreenFlags()
        payload = extractPayload(intent)
        setContent {
            BedehyarTheme {
                val p = payload
                AlarmScreen(
                    name = p?.name ?: "",
                    amount = p?.amount ?: 0L,
                    iOwe = p?.iOwe ?: true,
                    note = p?.note ?: "",
                    onDismiss = { stopRinging() },
                    onSnooze = { snoozeAndStop(p) }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        payload = extractPayload(intent)
    }

    private fun applyLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Wake the screen up even if it was completely off.
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        @Suppress("DEPRECATION")
        val wl = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                PowerManager.ACQUIRE_CAUSES_WAKEUP or
                PowerManager.ON_AFTER_RELEASE,
            "bedehyar:alarmScreen"
        )
        wl.acquire(5 * 60_000L)
    }

    private fun extractPayload(intent: Intent?): Payload? {
        val ex = intent?.extras ?: return null
        val id = ex.getLong(EXTRA_ID, -1L)
        if (id == -1L && !ex.containsKey(EXTRA_NAME)) return null
        return Payload(
            id = id,
            name = ex.getString(EXTRA_NAME) ?: "",
            amount = ex.getLong(EXTRA_AMOUNT, 0L),
            iOwe = ex.getBoolean(EXTRA_IOWE, true),
            note = ex.getString(EXTRA_NOTE) ?: ""
        )
    }

    private fun stopRinging() {
        try {
            AlarmService.stop(this)
        } catch (_: Exception) {
        }
        try {
            stopService(Intent(this, AlarmService::class.java))
        } catch (_: Exception) {
        }
        finish()
    }

    private fun snoozeAndStop(p: Payload?) {
        try {
            if (p != null && p.id > 0) {
                val dao = AppDatabase.get(this).dao()
                val tx = runBlocking { dao.getById(p.id) }
                if (tx != null) {
                    val snoozed = tx.copy(dueAt = System.currentTimeMillis() + 10 * 60_000L)
                    runBlocking { dao.update(snoozed) }
                    AlarmScheduler.schedule(this, snoozed)
                }
            }
        } catch (_: Exception) {
        }
        stopRinging()
    }

    companion object {
        const val EXTRA_ID = "bedehyar.extra.ID"
        const val EXTRA_NAME = "bedehyar.extra.NAME"
        const val EXTRA_AMOUNT = "bedehyar.extra.AMOUNT"
        const val EXTRA_IOWE = "bedehyar.extra.IOWE"
        const val EXTRA_NOTE = "bedehyar.extra.NOTE"
    }
}
