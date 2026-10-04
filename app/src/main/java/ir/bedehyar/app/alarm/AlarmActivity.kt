package ir.bedehyar.app.alarm

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.bedehyar.app.MainActivity
import ir.bedehyar.app.data.DIRECTION_I_OWE
import ir.bedehyar.app.ui.theme.BedehyarTheme
import ir.bedehyar.app.ui.theme.DebtRed
import ir.bedehyar.app.ui.theme.Vazir
import ir.bedehyar.app.util.Jalali

/**
 * Full-screen alarm page shown over any app and over the lock screen
 * (setShowWhenLocked/setTurnScreenOn on API 27+, window flags on API 26).
 * Presents the due transaction and three large actions:
 *  «خاموش کردن» stops sound + vibration,
 *  «۱۰ دقیقه بعد» schedules a one-shot snooze (original due date untouched),
 *  «مشاهده جزئیات» opens the transaction page.
 */
class AlarmActivity : ComponentActivity() {

    data class Payload(
        val id: Long,
        val name: String,
        val amount: Long,
        val iOwe: Boolean,
        val note: String,
        val dueAt: Long
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
                    payload = p,
                    onDismiss = { stopRinging() },
                    onSnooze = { p2 ->
                        AlarmService.snooze(this, p2.id)
                        finish()
                    },
                    onDetails = { p2 ->
                        stopRingingOnly()
                        val i = Intent(this, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            .putExtra(MainActivity.EXTRA_ROUTE, "tx/${p2.id}")
                        startActivity(i)
                        finish()
                    }
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

        // Wake the screen even if it was completely off (API 26 path).
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
            note = ex.getString(EXTRA_NOTE) ?: "",
            dueAt = ex.getLong(EXTRA_DUE, 0L)
        )
    }

    private fun stopRingingOnly() {
        try { AlarmService.stop(this) } catch (_: Exception) {}
        try { stopService(Intent(this, AlarmService::class.java)) } catch (_: Exception) {}
    }

    private fun stopRinging() {
        stopRingingOnly()
        finish()
    }

    companion object {
        // Values kept identical to v1 so legacy pending alarms still work.
        const val EXTRA_ID = "bedehyar.extra.ID"
        const val EXTRA_NAME = "bedehyar.extra.NAME"
        const val EXTRA_AMOUNT = "bedehyar.extra.AMOUNT"
        const val EXTRA_IOWE = "bedehyar.extra.IOWE"
        const val EXTRA_NOTE = "bedehyar.extra.NOTE"
        const val EXTRA_DUE = "bedehyar.extra.DUE"
    }
}

@Composable
private fun AlarmScreen(
    payload: AlarmActivity.Payload?,
    onDismiss: () -> Unit,
    onSnooze: (AlarmActivity.Payload) -> Unit,
    onDetails: (AlarmActivity.Payload) -> Unit
) {
    val p = payload
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .background(DebtRed.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AlarmOn,
                    contentDescription = null,
                    tint = DebtRed,
                    modifier = Modifier.size(44.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "زمان سررسید رسیده است",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                fontFamily = Vazir
            )
            Spacer(Modifier.height(24.dp))

            if (p == null) {
                Text("اطلاعات یادآوری یافت نشد.", style = MaterialTheme.typography.bodyMedium)
            } else {
                androidx.compose.material3.Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (p.iOwe) "بدهی من به او" else "طلب من از او",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (p.iOwe) DebtRed else MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(p.name, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            Jalali.price(p.amount) + " " + Jalali.currencySuffix(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (p.iOwe) DebtRed else MaterialTheme.colorScheme.primary
                        )
                        if (p.dueAt > 0) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "سررسید: " + Jalali.formatDate(p.dueAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (p.note.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                p.note,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DebtRed)
            ) {
                Icon(Icons.Filled.Bedtime, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("خاموش کردن", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { if (p != null) onSnooze(p) else onDismiss() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            ) {
                Icon(Icons.Filled.AlarmOn, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("۱۰ دقیقه بعد", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { if (p != null) onDetails(p) else onDismiss() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            ) {
                Icon(Icons.Filled.OpenInNew, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("مشاهده جزئیات تراکنش", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
