package ir.bedehyar.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.bedehyar.app.data.DashboardStats
import ir.bedehyar.app.data.PersonWithBalance
import ir.bedehyar.app.ui.components.AppScaffold
import ir.bedehyar.app.ui.components.EmptyState
import ir.bedehyar.app.ui.components.FilterChipRow
import ir.bedehyar.app.ui.components.SectionTitle
import ir.bedehyar.app.ui.components.StatCard
import ir.bedehyar.app.ui.theme.CreditGreen
import ir.bedehyar.app.ui.theme.DebtRed
import ir.bedehyar.app.ui.theme.Vazir
import ir.bedehyar.app.util.Fmt
import ir.bedehyar.app.util.Jalali

/**
 * Dashboard: totals, net balance, open accounts, due/overdue counters,
 * nearest due date, searchable & filterable person list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenPerson: (Long) -> Unit,
    onOpenTx: (Long) -> Unit,
    onOpenEdit: (personId: Long, txId: Long, type: Int) -> Unit,
    onOpenReports: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val vm: HomeViewModel = viewModel()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val persons by vm.persons.collectAsStateWithLifecycle()
    val search by vm.search.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val sort by vm.sort.collectAsStateWithLifecycle()
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as ir.bedehyar.app.App
    val settings by app.settings.flow.collectAsStateWithLifecycle(ir.bedehyar.app.data.AppSettings())

    AppScaffold(
        title = "حساب‌یار",
        actions = {
            IconButton(onClick = onOpenReports) {
                Icon(Icons.Filled.BarChart, contentDescription = "گزارش‌ها")
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "تنظیمات")
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onOpenEdit(0L, 0L, -1) }) {
                Icon(Icons.Filled.Add, contentDescription = "ثبت بدهی یا طلب جدید")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp)
        ) {
            item {
                DashboardHeader(stats = stats, rial = settings.currencyRial)
            }
            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { vm.search.value = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("جست‌وجوی شخص…") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
            }
            item {
                FilterChipRow(
                    options = listOf(
                        "همه" to (filter == FILTER_ALL),
                        "بدهکاران" to (filter == FILTER_DEBTORS),
                        "طلبکاران" to (filter == FILTER_CREDITORS),
                        "تسویه‌شده‌ها" to (filter == FILTER_SETTLED),
                        "سررسید گذشته" to (filter == FILTER_OVERDUE)
                    ),
                    onSelect = { vm.filter.value = it },
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                )
            }
            item {
                FilterChipRow(
                    options = listOf(
                        "مرتب‌سازی: نام" to (sort == SORT_NAME),
                        "مبلغ" to (sort == SORT_AMOUNT),
                        "تاریخ" to (sort == SORT_DATE)
                    ),
                    onSelect = { vm.sort.value = it },
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                )
            }
            item { SectionTitle("اشخاص (${Jalali.faDigits(persons.size.toString())})") }

            if (persons.isEmpty()) {
                item {
                    EmptyState(
                        if (search.isBlank() && filter == FILTER_ALL)
                            "هنوز شخصی ثبت نشده است.\nبا دکمه + اولین بدهی یا طلب را اضافه کنید."
                        else "موردی با این شرایط پیدا نشد."
                    )
                }
            } else {
                items(persons, key = { it.person.id }) { p ->
                    PersonRow(p = p, rial = settings.currencyRial) { onOpenPerson(p.person.id) }
                }
            }
        }
    }
}

@Composable
private fun DashboardHeader(stats: DashboardStats?, rial: Boolean) {
    val s = stats
    val netColor = when {
        s == null -> MaterialTheme.colorScheme.onSurface
        s.net > 0 -> CreditGreen
        s.net < 0 -> DebtRed
        else -> MaterialTheme.colorScheme.onSurface
    }
    Column(Modifier.padding(top = 8.dp)) {
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                title = "مجموع طلب‌ها",
                value = Fmt.money(s?.totalCredit ?: 0L, rial),
                valueColor = CreditGreen,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "مجموع بدهی‌ها",
                value = Fmt.money(s?.totalDebt ?: 0L, rial),
                valueColor = DebtRed,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                title = "خالص حساب",
                value = (if ((s?.net ?: 0L) > 0) "+" else "") + Fmt.money(s?.net ?: 0L, rial),
                valueColor = netColor,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "افراد با حساب باز",
                value = Jalali.faDigits((s?.openPersons ?: 0).toString()),
                valueColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                title = "سررسیدهای امروز",
                value = Jalali.faDigits((s?.dueToday ?: 0).toString()),
                valueColor = if ((s?.dueToday ?: 0) > 0) DebtRed else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "سررسیدهای گذشته",
                value = Jalali.faDigits((s?.overdue ?: 0).toString()),
                valueColor = if ((s?.overdue ?: 0) > 0) DebtRed else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "نزدیک‌ترین سررسید",
                value = s?.nextDueDate?.let { Jalali.formatDate(it) } ?: "—",
                valueColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1.3f),
                sub = s?.nextDueDate?.let { Fmt.dueLabel(it, System.currentTimeMillis()) }
            )
        }
    }
}

@Composable
private fun PersonRow(p: PersonWithBalance, rial: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    p.person.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (p.person.phone.isNotBlank()) {
                        Text(
                            Jalali.faDigits(p.person.phone),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    if (p.overdueCount > 0) {
                        Text(
                            "${Jalali.faDigits(p.overdueCount.toString())} سررسید گذشته",
                            style = MaterialTheme.typography.labelSmall,
                            color = DebtRed
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                when {
                    p.net > 0 -> {
                        Text(
                            Fmt.money(p.net, rial),
                            style = MaterialTheme.typography.titleSmall,
                            color = CreditGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Text("طلب من از او", style = MaterialTheme.typography.labelSmall,
                            color = CreditGreen)
                    }
                    p.net < 0 -> {
                        Text(
                            Fmt.money(-p.net, rial),
                            style = MaterialTheme.typography.titleSmall,
                            color = DebtRed,
                            fontWeight = FontWeight.Bold
                        )
                        Text("بدهی من به او", style = MaterialTheme.typography.labelSmall,
                            color = DebtRed)
                    }
                    else -> {
                        Text(
                            "تسویه‌شده",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
