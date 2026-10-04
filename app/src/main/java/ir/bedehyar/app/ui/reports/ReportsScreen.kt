package ir.bedehyar.app.ui.reports

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.bedehyar.app.data.TxWithRemaining
import ir.bedehyar.app.ui.components.AppScaffold
import ir.bedehyar.app.ui.components.DatePill
import ir.bedehyar.app.ui.components.EmptyState
import ir.bedehyar.app.ui.components.JalaliDatePickerDialog
import ir.bedehyar.app.ui.components.SectionTitle
import ir.bedehyar.app.ui.components.SimpleBarChart
import ir.bedehyar.app.ui.components.StatCard
import ir.bedehyar.app.ui.components.StatusChip
import ir.bedehyar.app.ui.theme.CreditGreen
import ir.bedehyar.app.ui.theme.DebtRed
import ir.bedehyar.app.util.BackupManager
import ir.bedehyar.app.util.CsvExporter
import ir.bedehyar.app.util.Fmt
import ir.bedehyar.app.util.Jalali
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Financial reports: totals, chart, date/person/category filters, CSV export. */
@Composable
fun ReportsScreen(
    onBack: () -> Unit,
    onOpenTx: (Long) -> Unit,
    onOpenPerson: (Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vm: ReportsViewModel = viewModel()
    val rangeType by vm.rangeType.collectAsStateWithLifecycle()
    val customFrom by vm.customFrom.collectAsStateWithLifecycle()
    val customTo by vm.customTo.collectAsStateWithLifecycle()
    val personFilter by vm.personFilter.collectAsStateWithLifecycle()
    val categoryFilter by vm.categoryFilter.collectAsStateWithLifecycle()
    val listTab by vm.listTab.collectAsStateWithLifecycle()
    val totals by vm.totals.collectAsStateWithLifecycle()
    val chart by vm.chart.collectAsStateWithLifecycle()
    val txList by vm.txList.collectAsStateWithLifecycle()
    val settledList by vm.settledList.collectAsStateWithLifecycle()
    val overdueList by vm.overdueList.collectAsStateWithLifecycle()
    val persons by vm.persons.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val app = context.applicationContext as ir.bedehyar.app.App
    val settings by app.settings.flow.collectAsStateWithLifecycle(ir.bedehyar.app.data.AppSettings())
    val rial = settings.currencyRial

    val snackbar = remember { SnackbarHostState() }
    var personMenu by remember { mutableStateOf(false) }
    var categoryMenu by remember { mutableStateOf(false) }
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val csv = CsvExporter.transactionsCsv(context, currentList(txList, listTab, settledList, overdueList))
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { os ->
                            os.write(csv.toByteArray(Charsets.UTF_8))
                            os.flush()
                        } ?: error("نمی‌توان فایل را باز کرد")
                    }
                    snackbar.showSnackbar("فایل CSV ذخیره شد")
                } catch (e: Exception) {
                    snackbar.showSnackbar("خطا در ذخیره فایل: ${e.message}")
                }
            }
        }
    }

    AppScaffold(
        title = "گزارش‌های مالی",
        onBack = onBack,
        snackbarHostState = snackbar,
        actions = {
            IconButton(onClick = {
                csvLauncher.launch("hesabyar-report-${Jalali.todayFormatted()}.csv")
            }) { Icon(Icons.Filled.Download, contentDescription = "خروجی CSV") }
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 40.dp)
        ) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(selected = rangeType == RANGE_TODAY, onClick = { vm.rangeType.value = RANGE_TODAY }, label = { Text("امروز") })
                    FilterChip(selected = rangeType == RANGE_WEEK, onClick = { vm.rangeType.value = RANGE_WEEK }, label = { Text("این هفته") })
                    FilterChip(selected = rangeType == RANGE_MONTH, onClick = { vm.rangeType.value = RANGE_MONTH }, label = { Text("این ماه") })
                    FilterChip(selected = rangeType == RANGE_ALL, onClick = { vm.rangeType.value = RANGE_ALL }, label = { Text("همه") })
                    FilterChip(selected = rangeType == RANGE_CUSTOM, onClick = { vm.rangeType.value = RANGE_CUSTOM }, label = { Text("بازه دلخواه") })
                }
            }
            if (rangeType == RANGE_CUSTOM) {
                item {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DatePill(
                            label = "از: " + (if (customFrom > 0) Jalali.formatDate(customFrom) else "—"),
                            onClick = { showFromPicker = true }
                        )
                        DatePill(
                            label = "تا: " + (if (customTo > 0) Jalali.formatDate(customTo) else "—"),
                            onClick = { showToPicker = true }
                        )
                    }
                }
            }
            item {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box {
                        FilterChip(
                            selected = personFilter != 0L,
                            onClick = { personMenu = true },
                            label = {
                                Text(
                                    if (personFilter == 0L) "همه اشخاص"
                                    else persons.firstOrNull { it.person.id == personFilter }?.person?.name ?: "شخص"
                                )
                            },
                            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) }
                        )
                        androidx.compose.material3.DropdownMenu(
                            expanded = personMenu,
                            onDismissRequest = { personMenu = false }
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("همه اشخاص") },
                                onClick = { vm.personFilter.value = 0L; personMenu = false }
                            )
                            persons.forEach { p ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(p.person.name) },
                                    onClick = { vm.personFilter.value = p.person.id; personMenu = false }
                                )
                            }
                        }
                    }
                    Box {
                        FilterChip(
                            selected = categoryFilter != 0L,
                            onClick = { categoryMenu = true },
                            label = {
                                Text(
                                    if (categoryFilter == 0L) "همه دسته‌ها"
                                    else categories.firstOrNull { it.id == categoryFilter }?.name ?: "دسته"
                                )
                            },
                            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) }
                        )
                        androidx.compose.material3.DropdownMenu(
                            expanded = categoryMenu,
                            onDismissRequest = { categoryMenu = false }
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("همه دسته‌ها") },
                                onClick = { vm.categoryFilter.value = 0L; categoryMenu = false }
                            )
                            categories.forEach { c ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(c.name) },
                                    onClick = { vm.categoryFilter.value = c.id; categoryMenu = false }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(Modifier.padding(top = 8.dp)) {
                    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("طلب‌های ثبت‌شده", Fmt.money(totals.createdCredit, rial), CreditGreen, Modifier.weight(1f))
                        StatCard("بدهی‌های ثبت‌شده", Fmt.money(totals.createdDebt, rial), DebtRed, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("مجموع دریافت‌ها", Fmt.money(totals.received, rial), CreditGreen, Modifier.weight(1f))
                        StatCard("مجموع پرداخت‌ها", Fmt.money(totals.paid, rial), DebtRed, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard(
                            "تسویه‌شده در بازه",
                            Jalali.faDigits(totals.settledCount.toString()),
                            MaterialTheme.colorScheme.onSurface,
                            Modifier.weight(1f)
                        )
                        StatCard(
                            "سررسیدهای گذشته",
                            Jalali.faDigits(totals.overdueCount.toString()),
                            if (totals.overdueCount > 0) DebtRed else MaterialTheme.colorScheme.onSurface,
                            Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(10.dp))
                SectionTitle("نمودار روزانه")
                SimpleBarChart(
                    points = chart.map { Triple(it.bucketStart, it.received, it.paid) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(selected = listTab == 0, onClick = { vm.listTab.value = 0 },
                        label = { Text("تراکنش‌های بازه") })
                    FilterChip(selected = listTab == 1, onClick = { vm.listTab.value = 1 },
                        label = { Text("تسویه‌شده‌ها") })
                    FilterChip(selected = listTab == 2, onClick = { vm.listTab.value = 2 },
                        label = { Text("سررسیدهای گذشته") })
                }
            }

            val current = when (listTab) {
                1 -> settledList
                2 -> overdueList
                else -> txList
            }
            if (current.isEmpty()) {
                item { EmptyState("موردی برای نمایش نیست.") }
            } else {
                items(current, key = { it.tx.id }) { t ->
                    ReportTxRow(t, rial, onClick = { onOpenTx(t.tx.id) })
                }
            }
        }
    }

    if (showFromPicker) {
        JalaliDatePickerDialog(
            initialMillis = if (customFrom > 0) customFrom else System.currentTimeMillis(),
            onPick = { vm.customFrom.value = it; showFromPicker = false },
            onDismiss = { showFromPicker = false }
        )
    }
    if (showToPicker) {
        JalaliDatePickerDialog(
            initialMillis = if (customTo > 0) customTo else System.currentTimeMillis(),
            onPick = { vm.customTo.value = it; showToPicker = false },
            onDismiss = { showToPicker = false }
        )
    }
}

private fun currentList(
    txList: List<TxWithRemaining>,
    tab: Int,
    settled: List<TxWithRemaining>,
    overdue: List<TxWithRemaining>
): List<TxWithRemaining> = when (tab) {
    1 -> settled
    2 -> overdue
    else -> txList
}

@Composable
private fun ReportTxRow(t: TxWithRemaining, rial: Boolean, onClick: () -> Unit) {
    val accent = if (t.tx.direction == ir.bedehyar.app.data.DIRECTION_I_OWE) DebtRed else CreditGreen
    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t.personName, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.width(8.dp))
                    StatusChip(t.isSettled)
                }
                Text(
                    (if (t.tx.direction == ir.bedehyar.app.data.DIRECTION_I_OWE) "بدهی من" else "طلب من") +
                        " — " + Jalali.formatDate(t.tx.createdAt) +
                        (t.tx.dueDate?.let { " — سررسید: " + Jalali.formatDate(it) } ?: ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    Fmt.money(t.tx.amount, rial),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
                Text(
                    if (t.isSettled) "تسویه" else "مانده: " + Fmt.money(t.remaining, rial),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
