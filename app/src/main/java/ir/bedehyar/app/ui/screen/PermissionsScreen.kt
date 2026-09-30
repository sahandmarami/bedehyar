package ir.bedehyar.app.ui.screen

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import ir.bedehyar.app.TxViewModel
import ir.bedehyar.app.ui.theme.CreditContainer
import ir.bedehyar.app.ui.theme.CreditGreen
import ir.bedehyar.app.ui.theme.CreditOnContainer
import ir.bedehyar.app.ui.theme.DebtContainer
import ir.bedehyar.app.ui.theme.DebtOnContainer
import ir.bedehyar.app.ui.theme.DebtRed
import ir.bedehyar.app.ui.theme.InkSecondary

private data class PermItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val description: String,
    val granted: Boolean,
    val onEnable: (Context) -> Unit
)

@Composable
fun PermissionsScreen(
    vm: TxViewModel,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result re-checked on resume */ }

    val sdk = Build.VERSION.SDK_INT

    val items = remember(refresh) {
        buildList {
            // 1) Notifications (Android 13+)
            val notifGranted = sdk < 33 || ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            add(
                PermItem(
                    icon = Icons.Filled.Notifications,
                    title = "اجازهٔ اعلان",
                    description = "برای نمایش اعلان آلارم سررسید در نوار وضعیت لازم است (اندروید ۱۳ به بعد).",
                    granted = notifGranted,
                    onEnable = {
                        if (sdk >= 33) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
            )

            // 2) Exact alarm (Android 12+)
            val exactGranted = if (sdk >= 31) {
                try {
                    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    am.canScheduleExactAlarms()
                } catch (_: Exception) {
                    false
                }
            } else {
                true
            }
            add(
                PermItem(
                    icon = Icons.Filled.Alarm,
                    title = "آلارم و یادآوری دقیق",
                    description = "برای زمان‌بندی دقیق آلارم و عبور از حالت صرفه‌جویی باتری (اندروید ۱۲ به بعد).",
                    granted = exactGranted,
                    onEnable = {
                        try {
                            it.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                    .setData(Uri.parse("package:${it.packageName}"))
                            )
                        } catch (_: Exception) {
                        }
                    }
                )
            )

            // 3) Draw overlays (all versions)
            val overlayGranted = Settings.canDrawOverlays(context)
            add(
                PermItem(
                    icon = Icons.Filled.OpenWith,
                    title = "نمایش روی برنامه‌های دیگر",
                    description = "برای باز شدن خودکار صفحهٔ آلارم روی هر برنامه یا صفحه‌ای که در آن هستی لازم است.",
                    granted = overlayGranted,
                    onEnable = {
                        try {
                            it.startActivity(
                                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                    .setData(Uri.parse("package:${it.packageName}"))
                            )
                        } catch (_: Exception) {
                        }
                    }
                )
            )

            // 4) Full screen intent (Android 14+)
            val fsiGranted = if (sdk >= 34) {
                try {
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.canUseFullScreenIntent()
                } catch (_: Exception) {
                    false
                }
            } else {
                true
            }
            add(
                PermItem(
                    icon = Icons.Filled.Layers,
                    title = "نمایش تمام‌صفحه روی صفحهٔ قفل",
                    description = "برای نمایش آلارم تمام‌صفحه هنگام قفل بودن گوشی (اندروید ۱۴ به بعد). در صفحهٔ باز شده گزینهٔ Full-screen intent را فعال کن.",
                    granted = fsiGranted,
                    onEnable = {
                        try {
                            it.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, it.packageName)
                            )
                        } catch (_: Exception) {
                        }
                    }
                )
            )

            // 5) Battery optimization (all versions)
            val batteryGranted = try {
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                pm.isIgnoringBatteryOptimizations(context.packageName)
            } catch (_: Exception) {
                false
            }
            add(
                PermItem(
                    icon = Icons.Filled.BatteryAlert,
                    title = "معافیت از بهینه‌سازی باتری",
                    description = "تا سیستم آلارم برنامه را در پس‌زمینه نکشد و آلارم همیشه دقیق پخش شود.",
                    granted = batteryGranted,
                    onEnable = {
                        try {
                            it.startActivity(
                                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                                    .setData(Uri.parse("package:${it.packageName}"))
                            )
                        } catch (_: Exception) {
                        }
                    }
                )
            )
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Text("مجوزهای آلارم", fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            "برای اینکه آلارم دقیقاً مثل آلارم ساعت گوشی کار کند، مجوزهای زیر را فعال کن. این کار فقط یک بار لازم است.",
            fontSize = 13.sp,
            color = InkSecondary
        )
        Spacer(Modifier.height(16.dp))

        items.forEach { item ->
            PermCard(item = item)
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                vm.completeOnboarding()
                onDone()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("ادامه و شروع", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        TextButton(
            onClick = {
                vm.completeOnboarding()
                onDone()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("فعلاً بعداً", color = InkSecondary)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PermCard(item: PermItem) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(42.dp)
                    .background(
                        if (item.granted) CreditContainer else DebtContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    tint = if (item.granted) CreditGreen else DebtRed,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .background(
                                if (item.granted) CreditContainer else DebtContainer,
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            if (item.granted) "دارد" else "ندارد",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.granted) CreditOnContainer else DebtOnContainer
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(item.description, fontSize = 12.sp, color = InkSecondary)
                if (!item.granted) {
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = { item.onEnable(context) }) {
                        Text("فعال‌سازی", color = CreditGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

