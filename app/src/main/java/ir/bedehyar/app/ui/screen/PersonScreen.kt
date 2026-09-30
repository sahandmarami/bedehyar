package ir.bedehyar.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.bedehyar.app.TxViewModel
import ir.bedehyar.app.data.DebtTransaction
import ir.bedehyar.app.ui.components.ConfirmDialog
import ir.bedehyar.app.ui.components.PayDialog
import ir.bedehyar.app.ui.theme.CreditContainer
import ir.bedehyar.app.ui.theme.CreditGreen
import ir.bedehyar.app.ui.theme.CreditOnContainer
import ir.bedehyar.app.ui.theme.DebtContainer
import ir.bedehyar.app.ui.theme.DebtOnContainer
import ir.bedehyar.app.ui.theme.DebtRed
import ir.bedehyar.app.ui.theme.InkSecondary
import ir.bedehyar.app.util.Jalali

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonScreen(
    vm: TxViewModel,
    personName: String,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit
) {
    val txsFlow = remember(personName) { vm.txsForFlow(personName) }
    val txs by txsFlow.collectAsState(initial = emptyList())

    var payFor by remember { mutableStateOf<DebtTransaction?>(null) }
    var deleteFor by remember { mutableStateOf<DebtTransaction?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(personName, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1)
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
        if (txs.isEmpty()) {
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("موردی برای این شخص ثبت نشده است", color = InkSecondary, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(txs, key = { it.id }) { tx ->
                    TxCard(
                        tx = tx,
                        onPay = { payFor = tx },
                        onEdit = { onEdit(tx.id) },
                        onDelete = { deleteFor = tx }
                    )
                }
            }
        }
    }

    payFor?.let { tx ->
        PayDialog(
            personName = tx.personName,
            remaining = tx.remaining,
            onConfirm = { amount ->
                vm.pay(tx, amount)
                payFor = null
            },
            onDismiss = { payFor = null }
        )
    }

    deleteFor?.let { tx ->
        ConfirmDialog(
            title = "حذف تراکنش",
            message = "آیا از حذف این تراکنش مطمئنی؟ این عمل قابل بازگشت نیست.",
            onConfirm = {
                vm.delete(tx)
                deleteFor = null
            },
            onDismiss = { deleteFor = null }
        )
    }
}

@Composable
private fun TxCard(
    tx: DebtTransaction,
    onPay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = if (tx.iOwe) DebtRed else CreditGreen
    val container = if (tx.iOwe) DebtContainer else CreditContainer
    val onContainer = if (tx.iOwe) DebtOnContainer else CreditOnContainer
    val typeText = if (tx.iOwe) "من بدهکارم" else "او بدهکار است"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .background(container, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        typeText,
                        color = onContainer,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (tx.isSettled) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .background(CreditContainer, RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "تسویه شده",
                            color = CreditOnContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "${Jalali.price(tx.amount)} تومان",
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            if (!tx.isSettled) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "باقی‌مانده: ${Jalali.price(tx.remaining)} تومان",
                    fontSize = 13.sp,
                    color = InkSecondary
                )
            }

            if (tx.dueAt != null) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.DateRange,
                        contentDescription = null,
                        tint = InkSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        Jalali.formatDateTime(tx.dueAt),
                        fontSize = 12.sp,
                        color = InkSecondary
                    )
                    if (tx.alarmEnabled) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = "آلارم فعال",
                            tint = CreditGreen,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            if (tx.note.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(tx.note, fontSize = 13.sp, color = InkSecondary, maxLines = 3)
            }

            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!tx.isSettled) {
                    TextButton(onClick = onPay, enabled = tx.remaining > 0) {
                        Text("پرداخت", color = CreditGreen, fontWeight = FontWeight.Bold)
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "ویرایش", tint = InkSecondary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = DebtRed)
                }
            }
        }
    }
}
