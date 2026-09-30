package ir.bedehyar.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ir.bedehyar.app.ui.theme.CreditGreen
import ir.bedehyar.app.ui.theme.DebtRed
import ir.bedehyar.app.ui.theme.InkSecondary
import ir.bedehyar.app.util.Jalali
import java.util.Calendar
import java.util.Locale

/** Groups typed digits with commas while editing (e.g. 1,250,000). */
object GroupingTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.filter { it in '0'..'9' }
        val formatted = if (digits.isEmpty()) "" else String.format(Locale.US, "%,d", digits.toLong())

        fun toTransformed(offset: Int): Int {
            if (formatted.isEmpty()) return 0
            var count = 0
            var i = 0
            val target = offset.coerceAtMost(digits.length)
            while (i < formatted.length && count < target) {
                if (formatted[i] != ',') count++
                i++
            }
            return i
        }

        fun toOriginal(offset: Int): Int {
            var count = 0
            var i = 0
            while (i < offset && i < formatted.length) {
                if (formatted[i] != ',') count++
                i++
            }
            return count
        }

        return TransformedText(
            AnnotatedString(formatted),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = toTransformed(offset)
                override fun transformedToOriginal(offset: Int): Int = toOriginal(offset)
            }
        )
    }
}

@Composable
fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "مبلغ (تومان)",
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            val normalized = Jalali.normalizeDigits(raw).filter { it in '0'..'9' }.take(15)
            onValueChange(normalized)
        },
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        enabled = enabled,
        singleLine = true,
        visualTransformation = GroupingTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

/** Persian (Jalali) month-grid date picker. */
@Composable
fun JalaliDatePickerDialog(
    initialJy: Int,
    initialJm: Int,
    initialJd: Int,
    onPick: (Int, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var viewYear by remember { mutableStateOf(initialJy) }
    var viewMonth by remember { mutableStateOf(initialJm) }
    var selectedDay by remember { mutableStateOf<Int?>(initialJd) }

    fun weekOffsetOf(jy: Int, jm: Int): Int {
        val g = Jalali.toGregorian(jy, jm, 1)
        val c = Calendar.getInstance()
        c.clear()
        c.set(g[0], g[1] - 1, g[2])
        return Jalali.weekdayIndex(c.get(Calendar.DAY_OF_WEEK))
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(18.dp)) {
                // Header: prev/next month + title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        if (viewMonth == 1) { viewMonth = 12; viewYear-- } else viewMonth--
                    }) {
                        Icon(Icons.Filled.KeyboardArrowRight, "ماه قبل")
                    }
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            Jalali.monthNames[viewMonth - 1],
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            Jalali.faDigits(viewYear.toString()),
                            fontSize = 13.sp,
                            color = InkSecondary
                        )
                    }
                    IconButton(onClick = {
                        if (viewMonth == 12) { viewMonth = 1; viewYear++ } else viewMonth++
                    }) {
                        Icon(Icons.Filled.KeyboardArrowLeft, "ماه بعد")
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Weekday header
                Row(Modifier.fillMaxWidth()) {
                    Jalali.weekDaysShort.forEach { d ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(d, fontSize = 12.sp, color = InkSecondary, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                val monthLen = Jalali.monthLength(viewYear, viewMonth)
                val offset = weekOffsetOf(viewYear, viewMonth)
                val cells: List<Int?> = List(offset) { null } + (1..monthLen).toList()
                val today = Jalali.todayJalali()

                cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { day ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (day != null) {
                                    val isSelected = day == selectedDay
                                    val isToday = viewYear == today[0] && viewMonth == today[1] && day == today[2]
                                    Box(
                                        Modifier
                                            .aspectRatio(1f)
                                            .then(
                                                if (isSelected) Modifier.background(CreditGreen, CircleShape)
                                                else if (isToday) Modifier.border(1.5.dp, CreditGreen, CircleShape)
                                                else Modifier
                                            )
                                            .clickable { selectedDay = day },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            Jalali.faDigits(day.toString()),
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                        repeat(7 - week.size) { Box(Modifier.weight(1f)) }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        val t = Jalali.todayJalali()
                        viewYear = t[0]; viewMonth = t[1]; selectedDay = t[2]
                    }) { Text("امروز") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("انصراف") }
                    Spacer(Modifier.width(6.dp))
                    Button(
                        onClick = {
                            val d = selectedDay
                            if (d != null) onPick(viewYear, viewMonth, d)
                        },
                        enabled = selectedDay != null
                    ) { Text("تأیید") }
                }
            }
        }
    }
}

/** Material 3 time picker wrapped in a dialog (24-hour format). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onPick: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("انتخاب ساعت", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))
                TimePicker(state = state)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("انصراف") }
                    Spacer(Modifier.width(6.dp))
                    Button(onClick = { onPick(state.hour, state.minute) }) { Text("تأیید") }
                }
            }
        }
    }
}

/** Register a (partial or full) payment for a transaction. */
@Composable
fun PayDialog(
    personName: String,
    remaining: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    val parsed = amount.toLongOrNull() ?: 0L

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("ثبت پرداخت", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "پرداخت برای $personName — باقی‌مانده: ${Jalali.price(remaining)} تومان",
                    fontSize = 13.sp,
                    color = InkSecondary
                )
                Spacer(Modifier.height(14.dp))
                AmountField(value = amount, onValueChange = { amount = it })
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        amount = (remaining / 2).toString()
                    }) { Text("نصف مبلغ") }
                    TextButton(onClick = {
                        amount = remaining.toString()
                    }) { Text("کل باقی‌مانده") }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("انصراف") }
                    Spacer(Modifier.width(6.dp))
                    Button(
                        onClick = { onConfirm(parsed.coerceAtMost(remaining)) },
                        enabled = parsed > 0
                    ) { Text("ثبت پرداخت") }
                }
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "حذف",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = DebtRed)
                Spacer(Modifier.height(8.dp))
                Text(message, fontSize = 14.sp, textAlign = TextAlign.Start)
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("انصراف") }
                    Spacer(Modifier.width(6.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = DebtRed)
                    ) { Text(confirmLabel) }
                }
            }
        }
    }
}
