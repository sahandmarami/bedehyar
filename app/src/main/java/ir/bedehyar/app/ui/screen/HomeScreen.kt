package ir.bedehyar.app.ui.screen

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.bedehyar.app.TxViewModel
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
fun HomeScreen(
    vm: TxViewModel,
    onOpenPerson: (String) -> Unit,
    onAdd: () -> Unit,
    onOpenPerms: () -> Unit
) {
    val summary by vm.summary.collectAsState()
    val persons by vm.persons.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text("بدهیار", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                },
                actions = {
                    IconButton(onClick = onOpenPerms) {
                        Icon(Icons.Filled.Settings, contentDescription = "مجوزها و راهنما")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, contentDescription = "افزودن تراکنش")
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            SummaryCard(
                debt = summary.debt,
                credit = summary.credit,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Row(
                Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("افراد", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    "(${Jalali.faDigits(persons.size.toString())})",
                    color = InkSecondary,
                    fontSize = 14.sp
                )
            }

            if (persons.isEmpty()) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("هنوز تراکنشی ثبت نشده است", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "با دکمهٔ + اولین بدهی یا طلب را ثبت کن",
                        color = InkSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(persons, key = { it.name }) { p ->
                        PersonRow(person = p, onClick = { onOpenPerson(p.name) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(debt: Long, credit: Long, modifier: Modifier = Modifier) {
    val net = credit - debt
    val netColor = when {
        net > 0 -> CreditGreen
        net < 0 -> DebtRed
        else -> InkSecondary
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatBlock("بدهی‌های من", debt, DebtRed, Modifier.weight(1f))
            VerticalDivider()
            StatBlock("طلب‌های من", credit, CreditGreen, Modifier.weight(1f))
            VerticalDivider()
            StatBlock("خالص", net, netColor, Modifier.weight(1f), emphasize = true)
        }
    }
}

@Composable
private fun StatBlock(
    label: String,
    value: Long,
    color: Color,
    modifier: Modifier = Modifier,
    emphasize: Boolean = false
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = InkSecondary)
        Spacer(Modifier.height(4.dp))
        Text(
            Jalali.price(value),
            fontSize = if (emphasize) 16.sp else 15.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text("تومان", fontSize = 10.sp, color = InkSecondary)
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(44.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@Composable
private fun PersonRow(person: TxViewModel.PersonSummary, onClick: () -> Unit) {
    val accent = if (person.net >= 0) CreditGreen else DebtRed
    val container = if (person.net >= 0) CreditContainer else DebtContainer
    val onContainer = if (person.net >= 0) CreditOnContainer else DebtOnContainer
    val tag = when {
        person.net > 0 -> "طلب دارد"
        person.net < 0 -> "بدهکار است"
        else -> "تسویه"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(46.dp)
                    .background(container, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    person.name.trim().take(1).ifEmpty { "؟" },
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    person.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1
                )
                Text(
                    "${Jalali.faDigits(person.openCount.toString())} مورد باز",
                    fontSize = 12.sp,
                    color = InkSecondary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    Jalali.price(kotlin.math.abs(person.net)),
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(3.dp))
                Box(
                    Modifier
                        .background(container, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text(tag, fontSize = 11.sp, color = onContainer, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
