package ir.bedehyar.app.ui.person

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ir.bedehyar.app.App
import ir.bedehyar.app.data.TxWithRemaining
import ir.bedehyar.app.ui.components.AppScaffold
import ir.bedehyar.app.ui.components.AmountField
import ir.bedehyar.app.ui.components.ConfirmDialog
import ir.bedehyar.app.ui.components.DirectionBadge
import ir.bedehyar.app.ui.components.EmptyState
import ir.bedehyar.app.ui.components.SectionTitle
import ir.bedehyar.app.ui.components.StatCard
import ir.bedehyar.app.ui.components.StatusChip
import ir.bedehyar.app.ui.theme.CreditGreen
import ir.bedehyar.app.ui.theme.DebtRed
import ir.bedehyar.app.util.Fmt
import ir.bedehyar.app.util.Jalali

/** Person page: balances, transaction & payment history, filters, person editing. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonScreen(
    personId: Long,
    onBack: () -> Unit,
    onOpenTx: (Long) -> Unit,
    onOpenEdit: (personId: Long, txId: Long, type: Int) -> Unit
) {
    val app = LocalContext.current.applicationContext as App
    val vm: PersonViewModel = viewModel(
        key = "person-$personId",
        factory = viewModelFactory {
            initializer { PersonViewModel(app, personId) }
        }
    )
    val person by vm.person.collectAsStateWithLifecycle()
    val balance by vm.balance.collectAsStateWithLifecycle()
    val txs by vm.transactions.collectAsStateWithLifecycle()
    val payments by vm.payments.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val settings by app.settings.flow.collectAsStateWithLifecycle(ir.bedehyar.app.data.AppSettings())
    val rial = settings.currencyRial

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); vm.consumeMessage() }
    }

    var showEdit by remember { mutableStateOf(false) }
    var showDeletePerson by remember { mutableStateOf(false) }
    var showQuickPay by remember { mutableStateOf(false) }

    AppScaffold(
        title = person?.name ?: "شخص",
        onBack = onBack,
        snackbarHostState = snackbar,
        actions = {
            IconButton(onClick = { showEdit = true }) {
                Icon(Icons.Filled.Edit, contentDescription = "ویرایش اطلاعات شخص")
            }
            IconButton(onClick = { showDeletePerson = true }) {
                Icon(Icons.Filled.Delete, contentDescription = "حذف شخص", tint = DebtRed)
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onOpenEdit(personId, 0L, -1) }) {
                Icon(Icons.Filled.Add, contentDescription = "تراکنش جدید برای این شخص")
            }
        }
    ) { padding ->
        val b = balance
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp)
        ) {
            item {
                Column(Modifier.padding(top = 4.dp)) {
                    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard(
                            title = "بدهکاری او",
                            value = Fmt.money(b?.creditRemaining ?: 0L, rial),
                            valueColor = CreditGreen,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "بستانکاری او",
                            value = Fmt.money(b?.debtRemaining ?: 0L, rial),
                            valueColor = DebtRed,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard(
                            title = "مانده حساب",
                            value = (if ((b?.net ?: 0L) > 0) "+" else "") +
                                Fmt.money(b?.net ?: 0L, rial),
                            valueColor = when {
                                (b?.net ?: 0L) > 0 -> CreditGreen
                                (b?.net ?: 0L) < 0 -> DebtRed
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "تراکنش‌های باز",
                            value = Jalali.faDigits((b?.openCount ?: 0).toString()),
                            valueColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            sub = person?.phone?.takeIf { it.isNotBlank() }
                                ?.let { "تماس: " + Jalali.faDigits(it) }
                        )
                    }
                }
            }

            item {
                // ---------- اقدام سریع: پرداخت جزئی + تراکنش جدید (کم‌ارتفاع و هم‌اندازه) ----------
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showQuickPay = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Icon(
                            Icons.Filled.RemoveCircleOutline, contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("ثبت پرداخت", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { onOpenEdit(personId, 0L, -1) },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Icon(
                            Icons.Filled.Add, contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("تراکنش جدید", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    }
                }
            }

            if (txs.isEmpty() && payments.isEmpty()) {
                item { EmptyState("اولین بدهی یا طلب را با دکمه + ثبت کنید") }
            } else {
                if (txs.isNotEmpty()) {
                    item { SectionTitle("تراکنش‌ها") }
                    items(txs, key = { it.tx.id }) { t ->
                        TxRow(t, rial) { onOpenTx(t.tx.id) }
                    }
                }
                if (payments.isNotEmpty()) {
                    item { SectionTitle("پرداخت‌ها") }
                    items(payments, key = { it.payment.id }) { pw ->
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        Fmt.money(pw.payment.amount, rial),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (pw.direction == ir.bedehyar.app.data.DIRECTION_I_OWE)
                                            DebtRed else CreditGreen
                                    )
                                    Text(
                                        (if (pw.direction == ir.bedehyar.app.data.DIRECTION_I_OWE)
                                            "پرداخت" else "دریافت") + " — " +
                                            Jalali.formatDateLong(pw.payment.date) +
                                            if (pw.payment.note.isNotBlank()) " — ${pw.payment.note}" else "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    "تراکنش #" + Jalali.faDigits(pw.payment.transactionId.toString()),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEdit) {
        person?.let { p ->
            EditPersonDialog(
                initialName = p.name,
                initialPhone = p.phone,
                initialNote = p.note,
                onConfirm = { name, phone, note ->
                    vm.savePerson(name, phone, note) { showEdit = false }
                },
                onDismiss = { showEdit = false }
            )
        }
    }
    if (showDeletePerson) {
        ConfirmDialog(
            title = "حذف شخص",
            text = "همه تراکنش‌ها و پرداخت‌های این شخص نیز حذف می‌شوند و یادآوری‌ها لغو می‌گردند. ادامه می‌دهید؟",
            confirmLabel = "حذف",
            danger = true,
            onConfirm = { vm.deletePerson { onBack() } },
            onDismiss = { showDeletePerson = false }
        )
    }
    if (showQuickPay) {
        QuickPayDialog(
            onConfirm = { amount, note ->
                showQuickPay = false
                vm.recordQuickPayment(amount, note)
            },
            onDismiss = { showQuickPay = false }
        )
    }
}

@Composable
private fun TxRow(t: TxWithRemaining, rial: Boolean, onClick: () -> Unit) {
    val isDebt = t.tx.direction == ir.bedehyar.app.data.DIRECTION_I_OWE
    val accent = if (isDebt) DebtRed else CreditGreen
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
                    DirectionBadge(t.tx.direction)
                    Spacer(Modifier.width(6.dp))
                    StatusChip(t.isSettled)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    t.tx.description.ifBlank { "بدون توضیح" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "ثبت: " + Jalali.formatDate(t.tx.createdAt) +
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
                    if (t.isSettled) "تسویه کامل" else "مانده: " + Fmt.money(t.remaining, rial),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (t.isSettled) MaterialTheme.colorScheme.onSurfaceVariant else accent
                )
            }
        }
    }
}

@Composable
private fun QuickPayDialog(
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ثبت پرداخت جزئی") },
        text = {
            Column {
                Text(
                    "مبلغ واردشده به قدیمی‌ترین تراکنشِ باز این شخص اضافه می‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                AmountField(
                    label = "مبلغ (تومان)",
                    value = amount,
                    onValueChange = { amount = it }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("یادداشت (اختیاری)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(amount, note) }) { Text("ثبت پرداخت") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
private fun EditPersonDialog(
    initialName: String,
    initialPhone: String,
    initialNote: String,
    onConfirm: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var note by remember { mutableStateOf(initialNote) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ویرایش اطلاعات شخص") },
        text = {
            Column {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("نام") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone, onValueChange = { phone = it },
                    label = { Text("شماره تماس") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text("یادداشت") }, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(name, phone, note) }) { Text("ذخیره") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
