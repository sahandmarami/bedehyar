package ir.bedehyar.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ir.bedehyar.app.ui.theme.CreditGreen
import ir.bedehyar.app.util.Jalali

/**
 * Shamsi (Jalali) date picker dialog: month grid starting Saturday,
 * Persian month names, leap-year aware month lengths.
 */
@Composable
fun JalaliDatePickerDialog(
    initialMillis: Long,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val init = if (initialMillis > 0) Jalali.jalaliOf(initialMillis) else Jalali.todayJalali()
    var jy by remember { mutableIntStateOf(init[0]) }
    var jm by remember { mutableIntStateOf(init[1]) }
    var selected by remember { mutableStateOf(if (initialMillis > 0) initialMillis else 0L) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(Modifier.padding(18.dp)) {

                // Header: month navigation
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = {
                        if (jm == 1) { jm = 12; jy -= 1 } else jm -= 1
                    }) { Icon(Icons.Filled.ChevronRight, contentDescription = "ماه قبل") }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            Jalali.monthNames[jm - 1] + " " + Jalali.faDigits(jy.toString()),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    IconButton(onClick = {
                        if (jm == 12) { jm = 1; jy += 1 } else jm += 1
                    }) { Icon(Icons.Filled.ChevronLeft, contentDescription = "ماه بعد") }
                }

                Spacer(Modifier.height(8.dp))

                // Weekday header (Saturday-first)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Jalali.weekDaysShort.forEach {
                        Box(Modifier.width(34.dp), contentAlignment = Alignment.Center) {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                val days = (1..Jalali.monthLength(jy, jm)).toList()
                val firstDowIdx = run {
                    val millis = Jalali.millisFromJalali(jy, jm, 1, 12, 0)
                    val cal = java.util.Calendar.getInstance()
                    cal.timeInMillis = millis
                    Jalali.weekdayIndex(cal.get(java.util.Calendar.DAY_OF_WEEK))
                }
                val todayJ = Jalali.todayJalali()
                val cells: List<Int?> = List(firstDowIdx) { null } + days

                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                ) {
                    items(cells) { day ->
                        Box(
                            Modifier
                                .size(38.dp)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        day == null -> Color.Transparent
                                        selected > 0 && isSameDay(selected, jy, jm, day) -> CreditGreen
                                        todayJ[0] == jy && todayJ[1] == jm && todayJ[2] == day ->
                                            MaterialTheme.colorScheme.primaryContainer
                                        else -> Color.Transparent
                                    }
                                )
                                .then(
                                    if (day == null) Modifier else Modifier.clickable {
                                        selected = Jalali.millisFromJalali(jy, jm, day, 12, 0)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (day != null) {
                                Text(
                                    Jalali.faDigits(day.toString()),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = when {
                                        selected > 0 && isSameDay(selected, jy, jm, day) -> Color.White
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        jy = todayJ[0]; jm = todayJ[1]
                        selected = Jalali.millisFromJalali(todayJ[0], todayJ[1], todayJ[2], 12, 0)
                    }) { Text("امروز") }
                    Row {
                        TextButton(onClick = onDismiss) { Text("بی‌خیال") }
                        TextButton(
                            onClick = {
                                if (selected > 0) onPick(selected)
                                else onDismiss()
                            },
                            enabled = selected > 0
                        ) { Text("انتخاب") }
                    }
                }
            }
        }
    }
}

private fun isSameDay(millis: Long, jy: Int, jm: Int, day: Int): Boolean {
    val j = Jalali.jalaliOf(millis)
    return j[0] == jy && j[1] == jm && j[2] == day
}

/** 24-hour time picker dialog (uses the Material3 TimePicker). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onPick: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialHour.coerceIn(0, 23),
        initialMinute = initialMinute.coerceIn(0, 59),
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ساعت یادآوری") },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onPick(state.hour, state.minute) }) { Text("انتخاب") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("بی‌خیال") } }
    )
}
