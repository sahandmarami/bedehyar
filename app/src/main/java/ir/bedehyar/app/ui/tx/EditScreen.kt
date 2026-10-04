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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ir.bedehyar.app.App
import ir.bedehyar.app.data.DIRECTION_I_OWE
import ir.bedehyar.app.data.DIRECTION_OWES_ME
import ir.bedehyar.app.ui.components.AppScaffold
import ir.bedehyar.app.ui.components.AmountField
import ir.bedehyar.app.ui.components.JalaliDatePickerDialog
import ir.bedehyar.app.ui.components.SectionTitle
import ir.bedehyar.app.ui.components.TimePickerDialog
import ir.bedehyar.app.ui.theme.DebtRed
import ir.bedehyar.app.util.Jalali

/**
 * Register / edit a debt or credit transaction.
 * Validation messages, date and time pickers are fully Persian.
 */
@Composable
fun EditScreen(
    txId: Long,
    presetPersonId: Long,
    presetType: Int,
    onBack: () -> Unit,
    onOpenPerson: (Long) -> Unit
) {
    val app = LocalContext.current.applicationContext as App
    val vm: EditViewModel = viewModel(
        key = "edit-$txId-$presetPersonId",
        factory = viewModelFactory {
            initializer {
                EditViewModel(app, txId, presetPersonId, presetType)
            }
        }
    )

    val name by vm.name.collectAsStateWithLifecycle()
    val phone by vm.phone.collectAsStateWithLifecycle()
    val amountText by vm.amountText.collectAsStateWithLifecycle()
    val direction by vm.direction.collectAsStateWithLifecycle()
    val description by vm.description.collectAsStateWithLifecycle()
    val categoryId by vm.categoryId.collectAsStateWithLifecycle()
    val registerDate by vm.registerDate.collectAsStateWithLifecycle()
    val hasDue by vm.hasDue.collectAsStateWithLifecycle()
    val dueDate by vm.dueDate.collectAsStateWithLifecycle()
    val reminderEnabled by vm.reminderEnabled.collectAsStateWithLifecycle()
    val reminderHour by vm.reminderHour.collectAsStateWithLifecycle()
    val reminderMinute by vm.reminderMinute.collectAsStateWithLifecycle()
    val privateNote by vm.privateNote.collectAsStateWithLifecycle()
    val isSaving by vm.isSaving.collectAsStateWithLifecycle()
    val nameError by vm.nameError.collectAsStateWithLifecycle()
    val amountError by vm.amountError.collectAsStateWithLifecycle()
    val dueError by vm.dueError.collectAsStateWithLifecycle()
    val persons by vm.persons.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()

    var showRegisterPicker by remember { mutableStateOf(false) }
    var showDuePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var personMenu by remember { mutableStateOf(false) }
    var categoryMenu by remember { mutableStateOf(false) }

    val lockPerson = vm.isEdit || (presetPersonId > 0L)

    AppScaffold(
        title = if (vm.isEdit) "ویرایش تراکنش" else "ثبت بدهی یا طلب جدید",
        onBack = onBack
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            SectionTitle("شخص")
            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Box {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { vm.name.value = it; vm.loadedPersonName = "" },
                            label = { Text("نام شخص *") },
                            isError = nameError != null,
                            supportingText = nameError?.let { { Text(it) } },
                            singleLine = true,
                            readOnly = lockPerson,
                            enabled = !lockPerson,
                            trailingIcon = {
                                if (!lockPerson) {
                                    Icon(
                                        Icons.Filled.ArrowDropDown, contentDescription = null,
                                        modifier = Modifier.clickable { personMenu = true }
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = personMenu && !lockPerson,
                            onDismissRequest = { personMenu = false }
                        ) {
                            persons.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p.person.name) },
                                    onClick = {
                                        vm.name.value = p.person.name
                                        vm.loadedPersonId = p.person.id
                                        vm.loadedPersonName = p.person.name
                                        personMenu = false
                                    }
                                )
                            }
                            if (persons.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("شخصی ثبت نشده — نام جدید تایپ کنید") },
                                    onClick = { personMenu = false }
                                )
                            }
                        }
                    }
                    if (!lockPerson && !vm.isEdit) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { vm.phone.value = it },
                            label = { Text("شماره تماس (اختیاری)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            SectionTitle("مبلغ و نوع حساب")
            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(14.dp)) {
                    AmountField(
                        label = "مبلغ (${Jalali.currencySuffix()}) *",
                        value = amountText,
                        onValueChange = { vm.amountText.value = it },
                        isError = amountError != null,
                        supportingText = amountError
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = direction == DIRECTION_I_OWE,
                            onClick = { vm.direction.value = DIRECTION_I_OWE },
                            label = { Text("من بدهکارم") }
                        )
                        FilterChip(
                            selected = direction == DIRECTION_OWES_ME,
                            onClick = { vm.direction.value = DIRECTION_OWES_ME },
                            label = { Text("او به من بدهکار است") }
                        )
                    }
                    if (direction == DIRECTION_I_OWE) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "این مبلغ را از او قرض گرفته‌ام (پرداخت = تسویه بدهی من).",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "این مبلغ را به او داده‌ام و طلب من است (دریافت = تسویه طلب من).",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            SectionTitle("جزئیات")
            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(14.dp)) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { vm.description.value = it },
                        label = { Text("توضیحات") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    Spacer(Modifier.height(12.dp))
                    Box {
                        OutlinedTextField(
                            value = categories.firstOrNull { it.id == categoryId }?.name
                                ?: if (categoryId == 0L) "بدون دسته" else "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("دسته‌بندی") },
                            trailingIcon = {
                                Icon(
                                    Icons.Filled.ArrowDropDown, contentDescription = null,
                                    modifier = Modifier.clickable { categoryMenu = true }
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = categoryMenu,
                            onDismissRequest = { categoryMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("بدون دسته") },
                                onClick = { vm.categoryId.value = 0L; categoryMenu = false }
                            )
                            categories.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c.name) },
                                    onClick = { vm.categoryId.value = c.id; categoryMenu = false }
                                )
                            }
                        }
                    }
                }
            }

            SectionTitle("تاریخ‌ها و یادآوری")
            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(14.dp)) {
                    DateField(
                        label = "تاریخ ثبت",
                        valueText = Jalali.formatDateLong(registerDate),
                        onClick = { showRegisterPicker = true }
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("سررسید دارد", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = hasDue,
                            onCheckedChange = {
                                vm.hasDue.value = it
                                if (!it) vm.reminderEnabled.value = false
                            }
                        )
                    }
                    if (hasDue) {
                        DateField(
                            label = "تاریخ سررسید (شمسی)",
                            valueText = if (dueDate > 0) Jalali.formatDateLong(dueDate) else "انتخاب کنید",
                            onClick = { showDuePicker = true },
                            isError = dueError != null,
                            supportingText = dueError
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("یادآوری با آلارم", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = reminderEnabled,
                                onCheckedChange = { vm.reminderEnabled.value = it }
                            )
                        }
                        if (reminderEnabled) {
                            DateField(
                                label = "ساعت یادآوری",
                                valueText = Jalali.formatTime(reminderHour, reminderMinute),
                                onClick = { showTimePicker = true },
                                icon = Icons.Filled.AccessTime
                            )
                        }
                    }
                }
            }

            SectionTitle("یادداشت خصوصی")
            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(14.dp)) {
                    OutlinedTextField(
                        value = privateNote,
                        onValueChange = { vm.privateNote.value = it },
                        label = { Text("فقط برای خودم") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { vm.save(onSuccess = onBack) },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(52.dp)
            ) {
                Text(if (isSaving) "در حال ذخیره…" else "ذخیره")
            }
        }
    }

    if (showRegisterPicker) {
        JalaliDatePickerDialog(
            initialMillis = registerDate,
            onPick = { vm.registerDate.value = it; showRegisterPicker = false },
            onDismiss = { showRegisterPicker = false }
        )
    }
    if (showDuePicker) {
        JalaliDatePickerDialog(
            initialMillis = if (dueDate > 0) dueDate else System.currentTimeMillis(),
            onPick = { vm.dueDate.value = it; showDuePicker = false },
            onDismiss = { showDuePicker = false }
        )
    }
    if (showTimePicker) {
        TimePickerDialog(
            initialHour = reminderHour,
            initialMinute = reminderMinute,
            onPick = { h, m -> vm.reminderHour.value = h; vm.reminderMinute.value = m; showTimePicker = false },
            onDismiss = { showTimePicker = false }
        )
    }
}

@Composable
private fun DateField(
    label: String,
    valueText: String,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.CalendarMonth,
    isError: Boolean = false,
    supportingText: String? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    LaunchedEffect(pressed) { if (pressed) onClick() }
    OutlinedTextField(
        value = valueText,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        isError = isError,
        supportingText = supportingText?.let { { Text(it, color = DebtRed) } },
        interactionSource = interaction,
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    )
}
