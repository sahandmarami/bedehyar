package ir.bedehyar.app.util

import java.util.Calendar
import java.util.Locale

/**
 * Jalali (Persian/Shamsi) calendar conversion + Persian digit helpers.
 * Based on the well-known jdf.scr.ir algorithm (valid for years ~1178..1633).
 */
object Jalali {

    val monthNames = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    val weekDays = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")

    val weekDaysShort = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

    private val gDayOfYear = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)

    private val leapMod33 = intArrayOf(1, 5, 9, 13, 17, 22, 26, 30)

    fun toJalali(gy: Int, gm: Int, gd: Int): IntArray {
        var jy = if (gy <= 1600) 0 else 979
        val gy2 = if (gy <= 1600) gy - 621 else gy - 1600
        val gy3 = if (gm > 2) gy2 + 1 else gy2
        var days = 365 * gy2 + (gy3 + 3) / 4 - (gy3 + 99) / 100 + (gy3 + 399) / 400 -
            80 + gd + gDayOfYear[gm - 1]
        jy += 33 * (days / 12053)
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm = if (days < 186) 1 + days / 31 else 7 + (days - 186) / 30
        val jd = 1 + (if (days < 186) days % 31 else (days - 186) % 30)
        return intArrayOf(jy, jm, jd)
    }

    fun toGregorian(jy: Int, jm: Int, jd: Int): IntArray {
        var gy = if (jy <= 979) 621 else 1600
        val jy2 = if (jy <= 979) jy else jy - 979
        var days = 365 * jy2 + (jy2 / 33) * 8 + ((jy2 % 33) + 3) / 4 + 78 + jd +
            (if (jm < 7) (jm - 1) * 31 else (jm - 7) * 30 + 186)
        gy += 400 * (days / 146097)
        days %= 146097
        if (days > 36524) {
            gy += 100 * ((days - 1) / 36524)
            days = (days - 1) % 36524
            if (days >= 365) days++
        }
        gy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            gy += (days - 1) / 365
            days = (days - 1) % 365
        }
        var gd = days + 1
        val leap = (gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0)
        val monthLen = intArrayOf(31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gm = 0
        while (gm < 12 && gd > monthLen[gm]) {
            gd -= monthLen[gm]
            gm++
        }
        return intArrayOf(gy, gm + 1, gd)
    }

    fun isLeapJalali(jy: Int): Boolean = leapMod33.contains(jy % 33)

    fun monthLength(jy: Int, jm: Int): Int = when {
        jm <= 6 -> 31
        jm <= 11 -> 30
        else -> if (isLeapJalali(jy)) 30 else 29
    }

    /** java Calendar.DAY_OF_WEEK (1=Sunday..7=Saturday) -> index in weekDays (0=Saturday) */
    fun weekdayIndex(calendarDow: Int): Int = calendarDow % 7

    fun faDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') sb.append(('۰' + (ch - '0'))) else sb.append(ch)
        }
        return sb.toString()
    }

    /** Normalize Persian/Arabic digits to Latin digits */
    fun normalizeDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when {
                ch in '۰'..'۹' -> sb.append(('0' + (ch - '۰')))
                ch in '٠'..'٩' -> sb.append(('0' + (ch - '٠')))
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    /** 1234567 -> "۱٬۲۳۴٬۵۶۷" */
    fun price(value: Long): String =
        faDigits(String.format(Locale.US, "%,d", value).replace(",", "٬"))

    fun two(n: Int): String = String.format(Locale.US, "%02d", n)

    fun todayJalali(): IntArray {
        val c = Calendar.getInstance()
        return toJalali(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }

    /** "شنبه ۱۵ اردیبهشت ۱۴۰۳ - ساعت ۰۹:۳۰" */
    fun formatFullDateTime(millis: Long): String {
        val c = Calendar.getInstance()
        c.timeInMillis = millis
        val j = toJalali(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
        val wd = weekDays[weekdayIndex(c.get(Calendar.DAY_OF_WEEK))]
        val hm = two(c.get(Calendar.HOUR_OF_DAY)) + ":" + two(c.get(Calendar.MINUTE))
        return "$wd ${faDigits(j[2].toString())} ${monthNames[j[1] - 1]} ${faDigits(j[0].toString())} - ساعت ${faDigits(hm)}"
    }

    fun millisFromJalali(jy: Int, jm: Int, jd: Int, hour: Int, minute: Int): Long {
        val g = toGregorian(jy, jm, jd)
        val c = Calendar.getInstance()
        c.clear()
        c.set(g[0], g[1] - 1, g[2], hour, minute, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    fun jalaliOf(millis: Long): IntArray {
        val c = Calendar.getInstance()
        c.timeInMillis = millis
        return toJalali(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }

    /* ------------------------------------------------------------------ */
    /*  Extra helpers used by the v1.1 UI / storage layer                  */
    /* ------------------------------------------------------------------ */

    /** Short numeric date, e.g. «۱۴۰۵/۰۷/۱۰» */
    fun formatDate(millis: Long): String {
        if (millis <= 0L) return "—"
        val j = jalaliOf(millis)
        return faDigits(String.format(Locale.US, "%04d/%02d/%02d", j[0], j[1], j[2]))
    }

    /** «۱۰ مهر ۱۴۰۵» */
    fun formatDateLong(millis: Long): String {
        if (millis <= 0L) return "—"
        val j = jalaliOf(millis)
        return "${faDigits(j[2].toString())} ${monthNames[j[1] - 1]} ${faDigits(j[0].toString())}"
    }

    /** «۱۴۰۵/۰۷/۱۰ — ۰۹:۳۰» */
    fun formatDateTime(millis: Long): String {
        if (millis <= 0L) return "—"
        val c = Calendar.getInstance()
        c.timeInMillis = millis
        val hm = two(c.get(Calendar.HOUR_OF_DAY)) + ":" + two(c.get(Calendar.MINUTE))
        return formatDate(millis) + " — " + faDigits(hm)
    }

    fun formatTime(hour: Int, minute: Int): String =
        faDigits(two(hour) + ":" + two(minute))

    /** Parses «1403/07/10» (any digits) into epoch millis at local midnight. */
    fun parseDateToMillis(text: String): Long? {
        val norm = normalizeDigits(text).trim().replace('-', '/')
        val parts = norm.split('/')
        if (parts.size != 3) return null
        val (y, m, d) = parts.map { it.toIntOrNull() ?: return null }
        if (y !in 1..1600 || m !in 1..12 || d !in 1..monthLength(y, m)) return null
        return millisFromJalali(y, m, d, 0, 0)
    }

    /** Today's jalali date as «۱۴۰۵/۰۷/۱۰» */
    fun todayFormatted(): String = formatDate(System.currentTimeMillis())

    /** Currency suffix honouring nothing but the app language (Toman by default). */
    fun currencySuffix(): String = "تومان"

    /** Start of today (local midnight) in epoch millis. */
    fun startOfToday(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    /** jalali weekday index (0 = Saturday .. 6 = Friday) of a java DayOfWeek value (1..7). */
    fun isoDowToIndex(isoValue: Int): Int = (isoValue + 1) % 7
}
