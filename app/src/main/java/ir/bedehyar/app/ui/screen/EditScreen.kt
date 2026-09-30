package ir.bedehyar.app.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.bedehyar.app.TxViewModel
import ir.bedehyar.app.ui.components.AmountField
import ir.bedehyar.app.ui.components.JalaliDatePickerDialog
import ir.bedehyar.app.ui.components.AppTimePickerDialog
import ir.bedehyar.app.ui.theme.CreditContainer
import ir.bedehyar.app.ui.theme.CreditGreen
import ir.bedehyar.app.ui.theme.CreditOnContainer
import ir.bedehyar.app.ui.theme.DebtContainer
import ir.bedehyar.app.ui.theme.DebtOnContainer
import ir.bedehyar.app.ui.theme.DebtRed
import ir.bedehyar.app.ui.theme.InkSecondary
import ir.bedehyar.app.util.Jalali
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    vm: TxViewModel,
    txId: Long,
    onBack: () -> Unit
) {
    val existing = if (txId == -1L) null else vm.txById(txId)

    var name by remember(existing) { mutableStateOf(existing?.personName ?: "") }
    var amountRaw by remember(existing) { mutableStateOf(existing?.amount?.toString() ?: "") }
    var iOwe by remember(existing) { mutableStateOf(existing?.iOwe ?: true) }
    var note by remember(existing) { mutableStateOf(existing?.note ?: "") }
    var alarmOn by remember(existing) { mutableStateOf(existing?.alarmEnabled ?: false) }
    var dueAt by remember(existing) { mutableStateOf(existing?.dueAt) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var chosenDate by remember { mutableStateOf<IntArray?>(null) } // jy, jm, jd
    var chosenHour by remember { mutableIntStateOf(9) }
    var chosenMinute by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }

    val today = remember { Jalali.todayJalali() }
    val amount = amountRaw.toLongOrNull() ?: 0L

    fun openTimeFrom(due: Long) {
        val c = Calendar.getInstance()
        c.timeInMillis = due
        chosenHour = c.get(Calendar.HOUR_OF_DAY)
        chosenMinute = c.get(Calendar.MINUTE)
        showTimePicker = true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (txId == -1L) "ثبت تراکنش جدید" else "ویرایش تراکنش",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("نام شخص") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) }
            )

            AmountField(value = amountRaw, onValueChange = { amountRaw = it })

            // Type selector
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TypeCard(
                    selected = iOwe,
                    onClick = { iOwe = true },
                    title = "من بدهکارم",
                    subtitle = "به او بدهکارم",
                    color = DebtRed,
                    container = DebtContainer,
                    onContainer = DebtOnContainer,
                    modifier = Modifier.weight(1f)
                )
                TypeCard(
                    selected = !iOwe,
                    onClick = { iOwe = false },
                    title = "او بدهکار است",
                    subtitle = "به من بدهکار است",
                    color = CreditGreen,
                    container = CreditContainer,
                    onContainer = CreditOnContainer,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("توضیحات (اختیاری)") },
                minLines = 2
            )

            // Alarm section
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("یادآوری با آلارم", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "در زمان سررسید، آلارم کامل پخش می‌شود",
                                fontSize = 11.sp,
                                color = InkSecondary
                            )
                        }
                        Switch(
                            checked = alarmOn,
                            onCheckedChange = { checked ->
                                alarmOn = checked
                                error = null
                                if (checked && dueAt == null) showDatePicker = true
                            }
                        )
                    }

                    if (alarmOn) {
                        Spacer(Modifier.height(10.dp))
                        val dueHm = dueAt?.let {
                            val c = java.util.Calendar.getInstance()
                            c.timeInMillis = it
                            "${Jalali.two(c.get(java.util.Calendar.HOUR_OF_DAY))}:${Jalali.two(c.get(java.util.Calendar.MINUTE))}"
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PickBox(
                                icon = { Icon(Icons.Filled.DateRange, null, tint = CreditGreen, modifier = Modifier.size(16.dp)) },
                                label = dueAt?.let { Jalali.formatDateTime(it) } ?: "انتخاب تاریخ سررسید",
                                modifier = Modifier.weight(1f),
                                onClick = { showDatePicker = true }
                            )
                            if (dueAt != null) {
                                PickBox(
                                    icon = { Icon(Icons.Filled.DateRange, null, tint = CreditGreen, modifier = Modifier.size(16.dp)) },
                                    label = dueHm?.let(Jalali::faDigits) ?: "--:--",
                                    modifier = Modifier.width(88.dp),
                                    onClick = { openTimeFrom(dueAt!!) }
                                )
                            }
                        }
                    }
                }
            }

            error?.let {
                Text(it, color = DebtRed, fontSize = 13.sp)
            }

            Button(
                onClick = {
                    when {
                        name.trim().isEmpty() -> error = "نام شخص را وارد کنید"
                        amount <= 0 -> error = "مبلغ معتبر وارد کنید"
                        alarmOn && dueAt == null -> error = "تاریخ و ساعت سررسید را انتخاب کنید"
                        alarmOn && (dueAt ?: 0L) <= System.currentTimeMillis() ->
                            error = "زمان سررسید باید در آینده باشد"
                        else -> {
                            vm.save(
                                id = txId,
                                name = name,
                                amount = amount,
                                iOwe = iOwe,
                                note = note,
                                dueAt = if (alarmOn) dueAt else null,
                                alarm = alarmOn
                            )
                            onBack()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("ذخیره", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(Modifier.height(12.dp))
        }
    }

    if (showDatePicker) {
        val initial = dueAt?.let { Jalali.jalaliOf(it) } ?: today
        JalaliDatePickerDialog(
            initialJy = initial[0],
            initialJm = initial[1],
            initialJd = initial[2],
            onPick = { jy, jm, jd ->
                chosenDate = intArrayOf(jy, jm, jd)
                dueAt?.let { openTimeFrom(it) }
                showDatePicker = false
                showTimePicker = true
            },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showTimePicker) {
        AppTimePickerDialog(
            initialHour = chosenHour,
            initialMinute = chosenMinute,
            onPick = { h, m ->
                chosenHour = h
                chosenMinute = m
                val d = chosenDate
                dueAt = if (d != null) {
                    Jalali.millisFromJalali(d[0], d[1], d[2], h, m)
                } else {
                    dueAt
                }
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

@Composable
private fun TypeCard(
    selected: Boolean,
    onClick: () -> Unit,
    title: String,
    subtitle: String,
    color: Color,
    container: Color,
    onContainer: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, color) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) container else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                color = if (selected) onContainer else InkSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                color = if (selected) onContainer.copy(alpha = 0.8f) else InkSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun PickBox(
    icon: @Composable () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}
