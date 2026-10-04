package ir.bedehyar.app.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.bedehyar.app.data.DashboardStats
import ir.bedehyar.app.data.PersonWithBalance
import ir.bedehyar.app.ui.components.AppScaffold
import ir.bedehyar.app.ui.theme.CreditGreen
import ir.bedehyar.app.ui.theme.DebtRed
import ir.bedehyar.app.ui.theme.InkPrimary
import ir.bedehyar.app.ui.theme.InkSecondary
import ir.bedehyar.app.util.Fmt
import ir.bedehyar.app.util.Jalali

/* ----------------------------------------------------------------------------
 * فاصله‌گذاری استاندارد — همه صفحه از این مقادیر پیروی می‌کند
 * -------------------------------------------------------------------------- */
private val PagePadding = 16.dp   // فاصله محتوا از لبه صفحه
private val CardGap = 12.dp       // فاصله بین کارت‌ها
private val SectionGap = 20.dp    // فاصله بین بخش‌ها
private val CardRadius = 20.dp    // گوشه گرد یکسان برای همه کارت‌ها
private val StatCardHeight = 104.dp
private val DueCardHeight = 88.dp
private val PersonCardHeight = 84.dp

/**
 * داشبورد — بازطراحی حرفه‌ای طبق مشخصات:
 * گرید ۲×۲ هم‌اندازه، سه کارت سررسید مساوی، فاصله‌گذاری یکنواخت،
 * نوار فیلتر اسکرول افقی، جست‌وجوی بزرگ، مرتب‌سازی جمع‌وجور، لیست اشخاص منظم.
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
            // دو آیکون هم‌اندازه با فاصله یکسان
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onOpenReports, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Filled.BarChart, contentDescription = "گزارش‌ها",
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = onOpenSettings, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Filled.Settings, contentDescription = "تنظیمات",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        floatingActionButton = {
            // دکمه + : گرد، سایه ملایم، فاصله استاندارد؛ با padding انتهای لیست
            // هیچ‌وقت روی کارت یا متن نمی‌نشیند.
            FloatingActionButton(
                onClick = { onOpenEdit(0L, 0L, -1) },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 4.dp, pressedElevation = 8.dp
                ),
                modifier = Modifier.size(58.dp)
            ) {
                Icon(
                    Icons.Filled.Add, contentDescription = "ثبت بدهی یا طلب جدید",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp)
        ) {
            // ---------- گرید ۲×۲ کارت‌های اصلی (هم‌عرض، هم‌ارتفاع) ----------
            item { DashboardHeader(stats = stats, rial = settings.currencyRial) }

            // ---------- جست‌وجو ----------
            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { vm.search.value = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PagePadding)
                        .padding(top = SectionGap)
                        .height(56.dp),
                    placeholder = {
                        Text(
                            "جست‌وجوی شخص...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = InkSecondary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search, contentDescription = null,
                            tint = InkSecondary, modifier = Modifier.size(24.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = InkPrimary)
                )
            }

            // ---------- نوار فیلتر (اسکرول افقی، بدون بریدگی) ----------
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = CardGap)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = PagePadding)
                ) {
                    val filterOptions = listOf(
                        "همه" to FILTER_ALL,
                        "بدهی‌ها" to FILTER_DEBTORS,
                        "طلب‌ها" to FILTER_CREDITORS,
                        "تسویه‌شده" to FILTER_SETTLED,
                        "موعد گذشته" to FILTER_OVERDUE
                    )
                    filterOptions.forEach { (label, id) ->
                        FilterChip(
                            selected = filter == id,
                            onClick = { vm.filter.value = id },
                            label = {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (filter == id)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else InkSecondary
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // ---------- عنوان لیست + مرتب‌سازی ----------
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PagePadding)
                        .padding(top = SectionGap)
                ) {
                    Text(
                        text = "اشخاص",
                        style = MaterialTheme.typography.titleMedium,
                        color = InkPrimary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "(${Jalali.faDigits(persons.size.toString())})",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                    Spacer(Modifier.weight(1f))
                    SortControl(current = sort, onSelect = { vm.sort.value = it })
                }
            }

            Spacer(Modifier.height(CardGap))

            if (persons.isEmpty()) {
                item {
                    EmptyPeopleState(searchBlank = search.isBlank() && filter == FILTER_ALL)
                }
            } else {
                items(persons, key = { it.person.id }) { p ->
                    PersonRow(p = p, rial = settings.currencyRial) {
                        onOpenPerson(p.person.id)
                    }
                }
            }
        }
    }
}

/* ----------------------------------------------------------------------------
 * کارت‌های آماری
 * -------------------------------------------------------------------------- */

@Composable
private fun DashboardHeader(stats: DashboardStats?, rial: Boolean) {
    val s = stats
    val netColor = when {
        s == null -> InkPrimary
        s.net > 0 -> CreditGreen
        s.net < 0 -> DebtRed
        else -> InkPrimary
    }
    Column(Modifier.padding(top = 8.dp)) {
        // ردیف اول: طلب‌ها / بدهی‌ها — دقیقاً هم‌عرض با weight(1f)
        Row(
            modifier = Modifier.padding(horizontal = PagePadding),
            horizontalArrangement = Arrangement.spacedBy(CardGap)
        ) {
            StatCardCentered(
                title = "مجموع طلب‌ها",
                value = Fmt.money(s?.totalCredit ?: 0L, rial),
                valueColor = CreditGreen,
                modifier = Modifier.weight(1f)
            )
            StatCardCentered(
                title = "مجموع بدهی‌ها",
                value = Fmt.money(s?.totalDebt ?: 0L, rial),
                valueColor = DebtRed,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(CardGap))
        // ردیف دوم: مانده / افراد — همان عرض و ارتفاع
        Row(
            modifier = Modifier.padding(horizontal = PagePadding),
            horizontalArrangement = Arrangement.spacedBy(CardGap)
        ) {
            StatCardCentered(
                title = "مانده حساب",
                value = (if ((s?.net ?: 0L) > 0) "+" else "") +
                    Fmt.money(s?.net ?: 0L, rial),
                valueColor = netColor,
                modifier = Modifier.weight(1f)
            )
            StatCardCentered(
                title = "افراد",
                value = Jalali.faDigits((s?.openPersons ?: 0).toString()) + " نفر",
                valueColor = InkPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(SectionGap))

        // ---------- سه کارت سررسید: دقیقاً هم‌اندازه، هیچ‌کدام بزرگ‌تر نیست ----------
        Row(
            modifier = Modifier.padding(horizontal = PagePadding),
            horizontalArrangement = Arrangement.spacedBy(CardGap)
        ) {
            DueCard(
                title = "امروز",
                value = Jalali.faDigits((s?.dueToday ?: 0).toString()),
                valueColor = if ((s?.dueToday ?: 0) > 0) InkPrimary else InkSecondary,
                modifier = Modifier.weight(1f)
            )
            DueCard(
                title = "گذشته",
                value = Jalali.faDigits((s?.overdue ?: 0).toString()),
                valueColor = if ((s?.overdue ?: 0) > 0) DebtRed else InkSecondary,
                modifier = Modifier.weight(1f)
            )
            DueCard(
                title = "نزدیک‌ترین موعد",
                value = s?.nextDueDate?.let { Jalali.formatDate(it) } ?: "—",
                valueColor = if (s?.nextDueDate != null) InkPrimary else InkSecondary,
                sub = s?.nextDueDate?.let { Fmt.dueLabel(it, System.currentTimeMillis()) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** کارت آماری: هم‌ارتفاع، محتوا کاملاً وسط‌چین (افقی و عمودی). */
@Composable
private fun StatCardCentered(
    title: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(StatCardHeight),
        shape = RoundedCornerShape(CardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = InkSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 17.sp, fontWeight = FontWeight.Bold
                ),
                color = valueColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** کارت سررسید: امروز / گذشته / نزدیک‌ترین موعد — همه هم‌اندازه و وسط‌چین. */
@Composable
private fun DueCard(
    title: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    sub: String? = null
) {
    Card(
        modifier = modifier.height(DueCardHeight),
        shape = RoundedCornerShape(CardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.sp, fontWeight = FontWeight.Bold
                ),
                color = valueColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (sub != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = sub,
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/* ----------------------------------------------------------------------------
 * مرتب‌سازی — کنترل جمع‌وجور «مرتب‌سازی: نام»
 * -------------------------------------------------------------------------- */

@Composable
private fun SortControl(current: Int, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val options = listOf(
        "نام" to SORT_NAME,
        "مبلغ" to SORT_AMOUNT,
        "تاریخ" to SORT_DATE
    )
    val currentLabel = options.firstOrNull { it.second == current }?.first ?: "نام"
    Box {
        Surface(
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp, MaterialTheme.colorScheme.outline
            ),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.clickable { open = true }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    Icons.Filled.Sort, contentDescription = null,
                    tint = InkSecondary, modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "مرتب‌سازی: $currentLabel",
                    style = MaterialTheme.typography.labelMedium,
                    color = InkSecondary
                )
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (label, id) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (id == current)
                                MaterialTheme.colorScheme.primary else InkPrimary
                        )
                    },
                    trailingIcon = {
                        if (id == current) {
                            Icon(
                                Icons.Filled.Check, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    onClick = {
                        onSelect(id)
                        open = false
                    }
                )
            }
        }
    }
}

/* ----------------------------------------------------------------------------
 * لیست اشخاص — کارت‌های هم‌ارتفاع با جای ثابت برای نام/شماره/مبلغ/وضعیت
 * -------------------------------------------------------------------------- */

@Composable
private fun PersonRow(p: PersonWithBalance, rial: Boolean, onClick: () -> Unit) {
    val amount = kotlin.math.abs(p.net)
    val (statusText, statusColor, amountColor) = when {
        p.overdueCount > 0 -> Triple("موعد گذشته", DebtRed, if (p.net > 0) CreditGreen else DebtRed)
        p.net > 0 -> Triple("طلب از او", CreditGreen, CreditGreen)
        p.net < 0 -> Triple("من به او بدهکارم", DebtRed, DebtRed)
        else -> Triple("تسویه‌شده", InkSecondary, InkSecondary)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PagePadding, vertical = CardGap / 2)
            .height(PersonCardHeight)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(CardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // سمت شروع: نام و شماره — جای ثابت
            Column(Modifier.weight(1f)) {
                Text(
                    p.person.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = InkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (p.person.phone.isNotBlank())
                        Jalali.faDigits(p.person.phone) else "بدون شماره",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            // سمت پایان: مبلغ و وضعیت — جای ثابت
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (amount > 0) Fmt.money(amount, rial) else "تسویه",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = amountColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = null,
                tint = InkSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/* ----------------------------------------------------------------------------
 * حالت خالی
 * -------------------------------------------------------------------------- */

@Composable
private fun EmptyPeopleState(searchBlank: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PagePadding, vertical = 40.dp)
    ) {
        if (searchBlank) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer, CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.People, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "هنوز کسی ثبت نشده است",
                style = MaterialTheme.typography.titleMedium,
                color = InkPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "با دکمه + اولین بدهی یا طلب را اضافه کن.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSecondary,
                textAlign = TextAlign.Center
            )
        } else {
            Icon(
                Icons.Filled.SearchOff, contentDescription = null,
                tint = InkSecondary, modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "شخصی با این شرایط پیدا نشد.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
