package ir.bedehyar.app.alarm

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.bedehyar.app.ui.theme.CreditContainer
import ir.bedehyar.app.ui.theme.CreditGreen
import ir.bedehyar.app.ui.theme.CreditOnContainer
import ir.bedehyar.app.ui.theme.DebtContainer
import ir.bedehyar.app.ui.theme.DebtRed
import ir.bedehyar.app.ui.theme.DebtOnContainer
import ir.bedehyar.app.ui.theme.InkSecondary
import ir.bedehyar.app.util.Jalali

@Composable
fun AlarmScreen(
    name: String,
    amount: Long,
    iOwe: Boolean,
    note: String,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    val accent = if (iOwe) DebtRed else CreditGreen
    val container = if (iOwe) DebtContainer else CreditContainer
    val onContainer = if (iOwe) DebtOnContainer else CreditOnContainer
    val titleText = if (iOwe) "سررسید بدهی" else "سررسید طلب"
    val descText = if (iOwe) "باید به $name بپردازی" else "$name باید به تو بپردازد"

    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "pulseScale"
    )

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            Box(
                Modifier
                    .size(130.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .background(container, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Alarm,
                    contentDescription = "آلارم",
                    tint = accent,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                titleText,
                color = accent,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                name,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(10.dp))

            Text(
                "${Jalali.price(amount)} تومان",
                color = accent,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp
            )

            Spacer(Modifier.height(8.dp))
            Text(descText, color = InkSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)

            if (note.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    note,
                    color = InkSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                )
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DebtRed,
                    contentColor = Color.White
                )
            ) {
                Text("خاموش کردن", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onSnooze,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(
                    Icons.Filled.Alarm,
                    contentDescription = null,
                    tint = onContainer,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("۱۰ دقیقه بعد", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}
