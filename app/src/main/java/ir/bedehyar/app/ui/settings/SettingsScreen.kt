package ir.bedehyar.app.ui.settings

import android.app.AlarmManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.bedehyar.app.data.THEME_DARK
import ir.bedehyar.app.data.THEME_LIGHT
import ir.bedehyar.app.data.THEME_SYSTEM
import ir.bedehyar.app.ui.components.AppScaffold
import ir.bedehyar.app.ui.components.DatePill
import ir.bedehyar.app.ui.components.SectionTitle
import ir.bedehyar.app.ui.components.TimePickerDialog
import ir.bedehyar.app.util.Jalali

/** Settings: currency, theme, reminder behavior, exact-alarm grant, backup/restore, about. */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vm: SettingsViewModel = viewModel()
    val settings by vm.settings.flow.collectAsStateWithLifecycle(ir.bedehyar.app.data.AppSettings())
    val busy by vm.busy.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); vm.consumeMessage() }
    }

    var showTimePicker by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var restorePreview by remember { mutableStateOf<String?>(null) }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { vm.exportBackup(it) } }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
        }
    }

    val scope = androidx.compose.runtime.rememberCoroutineScope()
    LaunchedEffect(pendingRestoreUri) {
        pendingRestoreUri?.let {
            restorePreview = vm.previewBackup(it)
        }
    }

    val am = context.getSystemService(android.content.Context.ALARM_SERVICE) as AlarmManager
    val exactAllowed = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()

    AppScaffold(
        title = "تنظیمات",
        onBack = onBack,
        snackbarHostState = snackbar
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 40.dp)
        ) {
            SectionTitle("نمایش")
            SettingsCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("واحد پول", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = !settings.currencyRial,
                            onClick = { vm.setRial(false) },
                            label = { Text("تومان") }
                        )
                        FilterChip(
                            selected = settings.currencyRial,
                            onClick = { vm.setRial(true) },
                            label = { Text("ریال") }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تم", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = settings.themeMode == THEME_SYSTEM,
                            onClick = { vm.setTheme(THEME_SYSTEM) }, label = { Text("خودکار") })
                        FilterChip(selected = settings.themeMode == THEME_LIGHT,
                            onClick = { vm.setTheme(THEME_LIGHT) }, label = { Text("روشن") })
                        FilterChip(selected = settings.themeMode == THEME_DARK,
                            onClick = { vm.setTheme(THEME_DARK) }, label = { Text("تاریک") })
                    }
                }
            }

            SectionTitle("یادآوری‌ها")
            SettingsCard {
                SettingSwitch(
                    title = "یادآوری سررسیدها",
                    checked = settings.remindersEnabled,
                    onChange = { vm.setReminders(it) }
                )
                SettingSwitch(
                    title = "صدای آلارم",
                    checked = settings.alarmSound,
                    onChange = { vm.setSound(it) }
                )
                SettingSwitch(
                    title = "ویبره",
                    checked = settings.vibration,
                    onChange = { vm.setVibration(it) }
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ساعت یادآوری", style = MaterialTheme.typography.bodyMedium)
                    DatePill(
                        label = Jalali.formatTime(settings.defaultReminderHour, settings.defaultReminderMinute),
                        onClick = { showTimePicker = true }
                    )
                }
            }

            if (!exactAllowed) {
                SectionTitle("آلارم دقیق")
                SettingsCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                Icons.Filled.Alarm, contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "برای یادآوری دقیق، دسترسی آلارم را فعال کنید",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (Build.VERSION.SDK_INT >= 31) {
                            Button(onClick = {
                                try {
                                    context.startActivity(
                                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                            .setData(android.net.Uri.parse("package:${context.packageName}"))
                                    )
                                } catch (_: Exception) {
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                                    } catch (_: Exception) {
                                    }
                                }
                            }) { Text("فعال‌سازی") }
                        }
                    }
                }
            }

            SectionTitle("داده‌ها")
            SettingsCard {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            backupLauncher.launch("hesabyar-backup-${Jalali.todayFormatted()}.json")
                        },
                        enabled = !busy
                    ) {
                        Icon(Icons.Filled.Backup, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("پشتیبان‌گیری")
                    }
                    OutlinedButton(
                        onClick = {
                            restoreLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                        },
                        enabled = !busy
                    ) {
                        Icon(Icons.Filled.Restore, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("بازیابی")
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "حساب‌یار — نسخه " + ir.bedehyar.app.BuildConfig.VERSION_NAME,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            initialHour = settings.defaultReminderHour,
            initialMinute = settings.defaultReminderMinute,
            onPick = { h, m -> vm.setDefaultTime(h, m); showTimePicker = false },
            onDismiss = { showTimePicker = false }
        )
    }

    restorePreview?.let { preview ->
        AlertDialog(
            onDismissRequest = { restorePreview = null; pendingRestoreUri = null },
            title = { Text("بازیابی نسخه پشتیبان") },
            text = { Text(preview) },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestoreUri?.let { vm.importBackup(it) }
                    restorePreview = null
                    pendingRestoreUri = null
                }) { Text("جایگزین کن") }
            },
            dismissButton = {
                TextButton(onClick = { restorePreview = null; pendingRestoreUri = null }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp), content = content)
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    subtitle: String? = null
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
