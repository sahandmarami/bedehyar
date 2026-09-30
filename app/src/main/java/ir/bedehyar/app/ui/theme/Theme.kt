package ir.bedehyar.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.bedehyar.app.R

val Vazir = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold)
)

val CreditGreen = Color(0xFF16A34A)
val CreditContainer = Color(0xFFDCFCE7)
val CreditOnContainer = Color(0xFF14532D)
val DebtRed = Color(0xFFDC2626)
val DebtContainer = Color(0xFFFEE2E2)
val DebtOnContainer = Color(0xFF7F1D1D)
val InkPrimary = Color(0xFF111827)
val InkSecondary = Color(0xFF64748B)

private val LightColors = lightColorScheme(
    primary = CreditGreen,
    onPrimary = Color.White,
    primaryContainer = CreditContainer,
    onPrimaryContainer = CreditOnContainer,
    secondary = Color(0xFF0D9488),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF134E4A),
    error = DebtRed,
    onError = Color.White,
    errorContainer = DebtContainer,
    onErrorContainer = DebtOnContainer,
    background = Color(0xFFF6F7F9),
    onBackground = InkPrimary,
    surface = Color.White,
    onSurface = InkPrimary,
    surfaceVariant = Color(0xFFEEF2F6),
    onSurfaceVariant = InkSecondary,
    outline = Color(0xFFD8DEE6),
    outlineVariant = Color(0xFFE5EAF0)
)

private fun vazirTypography(): Typography {
    val b = Typography()
    return b.copy(
        displayLarge = b.displayLarge.copy(fontFamily = Vazir),
        displayMedium = b.displayMedium.copy(fontFamily = Vazir),
        displaySmall = b.displaySmall.copy(fontFamily = Vazir),
        headlineLarge = b.headlineLarge.copy(fontFamily = Vazir),
        headlineMedium = b.headlineMedium.copy(fontFamily = Vazir),
        headlineSmall = b.headlineSmall.copy(fontFamily = Vazir),
        titleLarge = b.titleLarge.copy(fontFamily = Vazir),
        titleMedium = b.titleMedium.copy(fontFamily = Vazir),
        titleSmall = b.titleSmall.copy(fontFamily = Vazir),
        bodyLarge = b.bodyLarge.copy(fontFamily = Vazir),
        bodyMedium = b.bodyMedium.copy(fontFamily = Vazir),
        bodySmall = b.bodySmall.copy(fontFamily = Vazir),
        labelLarge = b.labelLarge.copy(fontFamily = Vazir),
        labelMedium = b.labelMedium.copy(fontFamily = Vazir),
        labelSmall = b.labelSmall.copy(fontFamily = Vazir)
    )
}

@Composable
fun BedehyarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = vazirTypography(),
        shapes = Shapes(
            small = RoundedCornerShape(10.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(24.dp)
        ),
        content = content
    )
}
