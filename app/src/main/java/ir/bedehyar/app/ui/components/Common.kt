package ir.bedehyar.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.bedehyar.app.data.DIRECTION_I_OWE
import ir.bedehyar.app.ui.theme.CreditGreen
import ir.bedehyar.app.ui.theme.DebtRed
import ir.bedehyar.app.ui.theme.Vazir
import ir.bedehyar.app.util.Fmt
import ir.bedehyar.app.util.Jalali

/* ------------------------------ scaffold ------------------------------ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    snackbarHostState: SnackbarHostState? = null,
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "بازگشت"
                            )
                        }
                    }
                },
                actions = { actions() }
            )
        },
        snackbarHost = { snackbarHostState?.let { SnackbarHost(it) } },
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton
    ) { padding ->
        content(padding)
    }
}

/* ------------------------------ states -------------------------------- */

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.Inbox, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorBox(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.ErrorOutline, contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(text, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
    }
}

/* ------------------------------ dialogs ------------------------------- */

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String = "تأیید",
    danger: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = if (danger) androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ) else androidx.compose.material3.ButtonDefaults.textButtonColors()
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

/* ------------------------------ inputs -------------------------------- */

/** Amount field with live thousand-separator grouping and Persian-digit tolerance. */
@Composable
fun AmountField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            val digits = ir.bedehyar.app.util.Jalali.normalizeDigits(raw).filter { it.isDigit() }
            if (digits.length <= 15) onValueChange(Fmt.groupDigits(digits))
        },
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        enabled = enabled,
        isError = isError,
        supportingText = supportingText?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

/* ------------------------------ chips/badges --------------------------- */

@Composable
fun DirectionBadge(direction: Int, modifier: Modifier = Modifier) {
    val isDebt = direction == DIRECTION_I_OWE
    Text(
        text = if (isDebt) "بدهی من" else "طلب من از او",
        style = MaterialTheme.typography.labelSmall,
        color = if (isDebt) DebtRed else CreditGreen,
        modifier = modifier
            .background(
                (if (isDebt) DebtRed else CreditGreen).copy(alpha = 0.12f),
                MaterialTheme.shapes.small
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
fun StatusChip(settled: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = if (settled) "تسویه‌شده" else "باز",
        style = MaterialTheme.typography.labelSmall,
        color = if (settled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
        modifier = modifier
            .background(
                (if (settled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
                    .copy(alpha = 0.10f),
                MaterialTheme.shapes.small
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
fun FilterChipRow(
    options: List<Pair<String, Boolean>>, // label, selected
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEachIndexed { i, (label, selected) ->
            FilterChip(
                selected = selected,
                onClick = { onSelect(i) },
                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

/* ------------------------------ money text ----------------------------- */

@Composable
fun MoneyText(
    amount: Long,
    rial: Boolean,
    color: Color,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium,
    modifier: Modifier = Modifier,
    withUnit: Boolean = true
) {
    Text(
        text = Fmt.money(amount, rial, withUnit),
        style = style,
        color = color,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

/* ------------------------------ stat card ------------------------------ */

@Composable
fun StatCard(
    title: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    sub: String? = null
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                color = valueColor,
                fontWeight = FontWeight.Bold
            )
            if (sub != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    sub,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

/* ------------------------------ bar chart ------------------------------ */

/**
 * Lightweight, dependency-free bar chart for reports:
 * [points] = (dayStartMillis, receivedValue, paidValue).
 */
@Composable
fun SimpleBarChart(
    points: List<Triple<Long, Long, Long>>,
    modifier: Modifier = Modifier
) {
    val green = CreditGreen
    val red = DebtRed
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val density = LocalDensity.current

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LegendDot(green, "دریافت‌ها")
                Spacer(Modifier.width(16.dp))
                LegendDot(red, "پرداخت‌ها")
            }
            Spacer(Modifier.height(12.dp))
            val maxV = points.maxOfOrNull { maxOf(it.second, it.third) } ?: 0L
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                if (points.isEmpty()) return@Canvas
                val slot = size.width / points.size
                val barW = slot / 3f
                val base = size.height - 22.dp.toPx()
                val maxPx = maxV.coerceAtLeast(1L).toFloat()
                points.forEachIndexed { i, p ->
                    val cx = slot * i + slot / 2f
                    val hR = base * (p.second / maxPx)
                    val hP = base * (p.third / maxPx)
                    drawRect(
                        color = green,
                        topLeft = Offset(cx - barW - 1.dp.toPx(), base - hR),
                        size = androidx.compose.ui.geometry.Size(barW, hR)
                    )
                    drawRect(
                        color = red,
                        topLeft = Offset(cx + 1.dp.toPx(), base - hP),
                        size = androidx.compose.ui.geometry.Size(barW, hP)
                    )
                    // day label (persian digit) every other bar
                    if (i % 2 == 0) {
                        val j = Jalali.jalaliOf(p.first)
                        val label = Jalali.faDigits(j[2].toString())
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            cx,
                            size.height - 4.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = labelColor.toArgbCompat()
                                textSize = with(density) { 9.dp.toPx() }
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun Color.toArgbCompat(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()
)

@Composable
fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

/** Small tappable date chip used in filter rows. */
@Composable
fun DatePill(label: String, onClick: () -> Unit) {
    androidx.compose.material3.Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.CalendarMonth, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
