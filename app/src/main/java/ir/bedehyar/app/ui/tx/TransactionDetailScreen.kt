package ir.bedehyar.app.ui.tx

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import ir.bedehyar.app.data.DIRECTION_I_OWE
import ir.bedehyar.app.data.Payment
import ir.bedehyar.app.ui.components.AppScaffold
import ir.bedehyar.app.ui.components.ConfirmDialog
import ir.bedehyar.app.ui.components.DirectionBadge
import ir.bedehyar.app.ui.components.EmptyState
import ir.bedehyar.app.ui.components.JalaliDatePickerDialog
import ir.bedehyar.app.ui.components.SectionTitle
import ir.bedehyar.app.ui.components.StatusChip
import ir.bedehyar.app.ui.theme.CreditGreen
import ir.bedehyar.app.ui.theme.DebtRed
import ir.bedehyar.app.util.Fmt
import ir.bedehyar.app.util.Jalali

/**
 * Transaction page: original amount, paid amount, remaining balance, full and
 * partial payment history with add/edit/delete, settle-full and alarm status.
 */
@Composable
fun TransactionDetailScreen(
    txId: Long,
    onBack: () -> Unit,
    onOpenPerson: (Long) -> Unit,
    onOpenEdit: (personId: Long, txId: Long, type: Int) -> Unit
) {
    val app = LocalContext.current.applicationContext as App
    val vm: TxDetailViewModel = viewModel(
        key = "tx-$txId",
        factory = viewModelFactory {
            initializer { TxDetailViewModel(app, txId) }
        }
    )
    val tw by vm.tx.collectAsStateWithLifecycle()
    val payments by vm.payments.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val settings by app.settings.flow.collectAsStateWithLifecycle(ir.bedehyar.app.data.AppSettings())
    val rial = settings.currencyRial

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    var showAddPayment by remember { mutableStateOf(false) }
    var showSettle by remember { mutableStateOf(false) }
    var showDeleteTx by remember { mutableStateOf(false) }
    var editPayment by remember { mutableStateOf<Payment?>(null) }
    var deletePayment by remember { mutableStateOf<Payment?>(null) }

    AppScaffold(
        title = "جزئیات تراکنش",
        onBack = onBack,
        snackbarHostState = snackbar,
        actions = {
            IconButton(onClick = { tw?.let { onOpenEdit(it.tx.personId, it.tx.id, it.tx.direction) } }) {
                Icon(Icons.Filled.Edit, contentDescription = "ویرایش")
            }
            IconButton(onClick = { showDeleteTx = true }) {
                Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = DebtRed)
            }
        }
    ) { padding ->
        if (tw == null) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.padding(padding)
            )
            return@AppScaffold
        }
        val item = tw!!
        val isDebt = item.tx.direction == DIRECTION_I_OWE
        val accent = if (isDebt) DebtRed else CreditGreen
        val progress = if (item.tx.amount > 0) {
            (item.paid.toFloat() / item.tx.amount.toFloat()).coerceIn(0f, 1f)
        } else 0f

        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 40.dp)
        ) {
            item {
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DirectionBadge(item.tx.direction)
                            Spacer(Modifier.width(8.dp))
                            StatusChip(item.isSettled)
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = if (item.isSettled) "" else Fmt.dueLabel(
                                    item.tx.dueDate ?: 0L, System.currentTimeMillis()
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = if ((item.tx.dueDate ?: Long.MAX_VALUE) < System.currentTimeMillis())
                                    DebtRed else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            item.personName,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.clickable { onOpenPerson(item.tx.personId) },
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                Fmt.money(item.tx.amount, rial),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = accent
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = accent,
                            trackColor = accent.copy(alpha = 0.15f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                "پرداخت‌شده: " + Fmt.money(item.paid, rial),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "مانده: " + Fmt.money(item.remaining, rial),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (item.remaining > 0) accent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        InfoLine("تاریخ ثبت:", Jalali.formatDateLong(item.tx.createdAt))
                        item.tx.dueDate?.let {
                            InfoLine(
                                "سررسید:",
                                Jalali.formatDateLong(it) + " — " +
                                    (if (item.tx.reminderEnabled)
                                        "یادآوری " + Jalali.formatTime(item.tx.reminderHour, item.tx.reminderMinute)
                                    else "بدون یادآوری")
                            )
                        }
                        item.categoryName?.let { InfoLine("دسته‌بندی:", it) }
                        if (item.tx.description.isNotBlank()) InfoLine("توضیحات:", item.tx.description)
                        if (item.tx.privateNote.isNotBlank()) InfoLine("یادداشت خصوصی:", item.tx.privateNote)
                    }
                }
            }

            item {
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { showAddPayment = true },
                        enabled = !item.isSettled && !busy,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Payments, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (isDebt) "ثبت پرداخت جزئی" else "ثبت دریافت جزئی")
                    }
                    Button(
                        onClick = { showSettle = true },
                        enabled = !item.isSettled && !busy,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Paid, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("تسویه کامل")
                    }
                }
            }

            item { SectionTitle("پرداخت‌ها و دریافت‌ها (${Jalali.faDigits(payments.size.toString())})") }

            if (payments.isEmpty()) {
                item { EmptyState("هنوز پرداختی ثبت نشده است.") }
            } else {
                items(payments, key = { it.id }) { pay ->
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    Fmt.money(pay.amount, rial),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = accent,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    Jalali.formatFullDateTime(pay.date) +
                                        if (pay.note.isNotBlank()) " — ${pay.note}" else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { editPayment = pay }) {
                                Icon(Icons.Filled.Edit, contentDescription = "ویرایش پرداخت")
                            }
                            IconButton(onClick = { deletePayment = pay }) {
                                Icon(Icons.Filled.Delete, contentDescription = "حذف پرداخت", tint = DebtRed)
                            }
                        }
                    }
                }
            }
        }
    }

    /* ---------------------------- dialogs ---------------------------- */

    if (showAddPayment) {
        val remaining = tw?.remaining ?: 0L
        PaymentDialog(
            title = if ((tw?.tx?.direction ?: 0) == DIRECTION_I_OWE) "ثبت پرداخت" else "ثبت دریافت",
            initialAmount = remaining,
            maxAmount = remaining,
            initialDate = System.currentTimeMillis(),
            initialNote = "",
            rial = rial,
            onConfirm = { amount, date, note ->
                vm.addPayment(amount, date, note) { ok -> if (ok) showAddPayment = false }
            },
            onDismiss = { showAddPayment = false }
        )
    }
    if (showSettle) {
        ConfirmDialog(
            title = "تسویه کامل",
            text = "مانده " + Fmt.money(tw?.remaining ?: 0L, rial) +
                " پرداخت/دریافت و تراکنش بسته می‌شود. یادآوری آن نیز لغو خواهد شد. ادامه می‌دهید؟",
            confirmLabel = "تسویه کن",
            onConfirm = {
                vm.settleFull(System.currentTimeMillis()) { ok -> if (ok) showSettle = false }
            },
            onDismiss = { showSettle = false }
        )
    }
    if (showDeleteTx) {
        ConfirmDialog(
            title = "حذف تراکنش",
            text = "تراکنش و همه پرداخت‌های آن حذف می‌شوند و این عمل برگشت‌پذیر نیست. ادامه می‌دهید؟",
            confirmLabel = "حذف",
            danger = true,
            onConfirm = { vm.deleteTransaction { onBack() } },
            onDismiss = { showDeleteTx = false }
        )
    }
    editPayment?.let { pay ->
        PaymentDialog(
            title = "ویرایش پرداخت",
            initialAmount = pay.amount,
            maxAmount = (tw?.tx?.amount ?: 0L),
            initialDate = pay.date,
            initialNote = pay.note,
            rial = rial,
            isEdit = true,
            onConfirm = { amount, date, note ->
                vm.updatePayment(pay.copy(amount = amount, date = date, note = note)) { ok ->
                    if (ok) editPayment = null
                }
            },
            onDismiss = { editPayment = null }
        )
    }
    deletePayment?.let { pay ->
        ConfirmDialog(
            title = "حذف پرداخت",
            text = "این پرداخت حذف می‌شود و مانده تراکنش مجدداً محاسبه می‌گردد. ادامه می‌دهید؟",
            confirmLabel = "حذف",
            danger = true,
            onConfirm = {
                vm.deletePayment(pay) {}
                deletePayment = null
            },
            onDismiss = { deletePayment = null }
        )
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.padding(top = 4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(6.dp))
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

/** Add/edit payment dialog with amount cap validation. */
@Composable
private fun PaymentDialog(
    title: String,
    initialAmount: Long,
    maxAmount: Long,
    initialDate: Long,
    initialNote: String,
    rial: Boolean,
    isEdit: Boolean = false,
    onConfirm: (Long, Long, String) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf(Fmt.groupDigits(initialAmount.toString())) }
    var date by remember { mutableStateOf(initialDate) }
    var note by remember { mutableStateOf(initialNote) }
    var showPicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { raw ->
                        val digits = ir.bedehyar.app.util.Jalali.normalizeDigits(raw).filter { it.isDigit() }
                        if (digits.length <= 15) amountText = Fmt.groupDigits(digits)
                    },
                    label = { Text("مبلغ (${Jalali.currencySuffix()})") },
                    isError = error != null,
                    supportingText = {
                        Text(
                            error ?: ("حداکثر: " + Fmt.money(maxAmount, rial)),
                            color = if (error != null) DebtRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true
                )
                Spacer(Modifier.height(10.dp))
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                LaunchedEffect(pressed) { if (pressed) showPicker = true }
                OutlinedTextField(
                    value = Jalali.formatDateLong(date),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("تاریخ") },
                    trailingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                    interactionSource = interaction,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("توضیحات پرداخت") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = Fmt.parseAmount(amountText)
                when {
                    amount <= 0 -> error = "مبلغ باید بزرگ‌تر از صفر باشد"
                    amount > maxAmount -> error = "مبلغ نمی‌تواند بیشتر از " + Fmt.money(maxAmount, rial) + " باشد"
                    else -> onConfirm(amount, date, note.trim())
                }
            }) { Text(if (isEdit) "ذخیره" else "ثبت") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
    if (showPicker) {
        JalaliDatePickerDialog(
            initialMillis = date,
            onPick = { date = it; showPicker = false },
            onDismiss = { showPicker = false }
        )
    }
}
