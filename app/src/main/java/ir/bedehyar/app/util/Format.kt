package ir.bedehyar.app.util

import ir.bedehyar.app.util.Jalali.faDigits
import java.util.Locale

/** Display formatting helpers (Persian digits, thousands separator, currency unit). */
object Fmt {

    /** 1234567 -> «۱٬۲۳۴٬۵۶۷ تومان» یا «۱۲٬۳۴۵٬۶۷۰ ریال» */
    fun money(amount: Long, rial: Boolean, withUnit: Boolean = true): String {
        val shown = if (rial) amount * 10L else amount
        val s = Jalali.price(shown)
        return if (withUnit) "$s ${if (rial) "ریال" else "تومان"}" else s
    }

    /** Plain grouped number without unit. */
    fun amount(amount: Long, rial: Boolean): String = money(amount, rial, withUnit = false)

    /** Parses a user-typed amount string (any digits, thousand separators) to Long Toman. */
    fun parseAmount(text: String): Long {
        val digits = Jalali.normalizeDigits(text).filter { it.isDigit() }
        return digits.toLongOrNull() ?: 0L
    }

    /** Groups a raw digit string for editing: "1234567" -> "1,234,567" */
    fun groupDigits(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        if (digits.isEmpty()) return ""
        val n = digits.toLongOrNull() ?: return digits
        return String.format(Locale.US, "%,d", n)
    }

    /** «۳ روز گذشته» / «امروز» / «۲ روز دیگر» */
    fun dueLabel(dueMillis: Long, now: Long): String {
        val dayMs = 86_400_000L
        val d = Math.floorDiv(dueMillis - now, dayMs).toInt()
        return when {
            d == 0 -> "امروز"
            d == 1 -> "فردا"
            d == -1 -> "دیروز"
            d > 1 -> "$d روز دیگر"
            else -> faDigits((-d).toString()) + " روز گذشته"
        }
    }
}
