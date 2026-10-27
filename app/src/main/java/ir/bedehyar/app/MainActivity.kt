package ir.bedehyar.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import ir.bedehyar.app.util.CrashReporter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.bedehyar.app.data.AppSettings
import ir.bedehyar.app.ui.AppRoot
import ir.bedehyar.app.ui.theme.BedehyarTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val startRoute = intent?.getStringExtra(EXTRA_ROUTE)
        setContent {
            val app = application as App
            val settings by app.settings.flow
                .map { it }
                .collectAsStateWithLifecycle(initialValue = AppSettings())
            BedehyarTheme(themeMode = settings.themeMode) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    // اگر خطای مرگبار قبلی ثبت شده باشد، ابتدا گزارش آن نشان داده
                    // می‌شود تا حتی در حالت کرش‌لقپ هم قابل خواندن/کپی باشد.
                    var crashReport by remember { mutableStateOf(CrashReporter.lastCrashForCurrentVersion(app)) }
                    if (crashReport != null) {
                        CrashReportDialog(
                            text = crashReport.orEmpty(),
                            onDismiss = {
                                CrashReporter.clear(app)
                                crashReport = null
                            }
                        )
                    } else {
                        AppRoot(startRoute = startRoute, settings = settings)
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_ROUTE = "bedehyar.extra.ROUTE"
    }
}

/** Full-screen-gated dialog that shows the last fatal error and lets the user copy it. */
@Composable
fun CrashReportDialog(text: String, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("گزارش خطای آخرین اجرا") },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text.take(4000),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "لطفاً با دکمهٔ کپی، متن را کپی و برای پشتیبانی بفرستید.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                clipboard.setText(AnnotatedString(text))
                onDismiss()
            }) { Text("کپی و بستن") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("بستن بدون کپی") }
        }
    )
}

/** Runtime permission + first-run guidance, rendered inside the app root. */
@Composable
fun PermissionsAndGuide(settings: AppSettings) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val scope = rememberCoroutineScope()

    // POST_NOTIFICATIONS (Android 13+)
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* denied: app keeps working, only reminders lose the notification part */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // فقط بعد از اولین خواندن واقعی DataStore تصمیم می‌گیریم؛
    // وگرنه مقدار پیش‌فرض (guideShown=false) باعث نمایش دوباره پیام در هر بار باز شدن می‌شد.
    var settingsLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        app.settings.flow.first()
        settingsLoaded = true
    }

    val showGuide = settingsLoaded && !settings.guideShown
    if (showGuide) {
        AlertDialog(
            onDismissRequest = {
                // بستن با لمس بیرون هم به عنوان «دیده شده» ثبت می‌شود
                scope.launch { app.settings.setGuideShown(true) }
            },
            title = { Text("خوش آمدید 👋") },
            text = {
                Column {
                    Text(
                        "حساب‌یار اطلاعات مالی شما را فقط روی همین گوشی نگه می‌دارد؛ هیچ داده‌ای به اینترنت ارسال نمی‌شود.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "برای یادآوری دقیق سررسیدها:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    GuideBullet("۱) اجازه نمایش اعلان را تأیید کنید.")
                    GuideBullet("۲) در تنظیمات، دسترسی «آلارم دقیق» را فعال کنید.")
                    GuideBullet("۳) در صورت نیاز، بهینه‌سازی باتری برای این برنامه را خاموش کنید.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { app.settings.setGuideShown(true) }
                }) { Text("متوجه شدم") }
            }
        )
    }
}

@Composable
private fun GuideBullet(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, start = 4.dp)
    )
}
