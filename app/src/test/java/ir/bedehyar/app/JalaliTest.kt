package ir.bedehyar.app

import ir.bedehyar.app.util.Jalali
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the Jalali (Shamsi) calendar conversion.
 * Anchor dates are verified against the official Iranian calendar:
 *   1403/01/01 = 2024-03-20, 1400/01/01 = 2021-03-21, 1398/01/01 = 2019-03-21,
 *   1402/01/01 = 2023-03-21, 1404/01/01 = 2025-03-21, 1405/01/01 = 2026-03-21
 */
class JalaliTest {

    @Test
    fun `nowruz anchors convert to expected jalali dates`() {
        assertArrayEquals(intArrayOf(1403, 1, 1), Jalali.toJalali(2024, 3, 20))
        assertArrayEquals(intArrayOf(1400, 1, 1), Jalali.toJalali(2021, 3, 21))
        assertArrayEquals(intArrayOf(1398, 1, 1), Jalali.toJalali(2019, 3, 21))
        assertArrayEquals(intArrayOf(1402, 1, 1), Jalali.toJalali(2023, 3, 21))
        assertArrayEquals(intArrayOf(1404, 1, 1), Jalali.toJalali(2025, 3, 21))
        assertArrayEquals(intArrayOf(1405, 1, 1), Jalali.toJalali(2026, 3, 21))
    }

    @Test
    fun `leap year esfand has 30 days and maps back to gregorian`() {
        assertTrue(Jalali.isLeapJalali(1403))
        assertFalse(Jalali.isLeapJalali(1404))
        assertEquals(30, Jalali.monthLength(1403, 12))
        assertEquals(29, Jalali.monthLength(1404, 12))
        assertArrayEquals(intArrayOf(2025, 3, 20), Jalali.toGregorian(1403, 12, 30))
    }

    @Test
    fun `month lengths follow the 31-30-29 pattern`() {
        assertEquals(31, Jalali.monthLength(1404, 1))
        assertEquals(31, Jalali.monthLength(1404, 6))
        assertEquals(30, Jalali.monthLength(1404, 7))
        assertEquals(30, Jalali.monthLength(1404, 11))
    }

    @Test
    fun `round trip over 100 years is lossless`() {
        val cal = java.util.Calendar.getInstance().apply {
            clear()
            set(1996, 2, 20) // 1996-03-20
        }
        val end = java.util.Calendar.getInstance().apply {
            clear()
            set(2096, 2, 20)
        }
        var guard = 0
        while (!cal.after(end) && guard < 40_000) {
            val gy = cal.get(java.util.Calendar.YEAR)
            val gm = cal.get(java.util.Calendar.MONTH) + 1
            val gd = cal.get(java.util.Calendar.DAY_OF_MONTH)
            val j = Jalali.toJalali(gy, gm, gd)
            val g = Jalali.toGregorian(j[0], j[1], j[2])
            assertEquals("$gy-$gm-$gd", intArrayOf(gy, gm, gd).contentToString(), g.contentToString())
            cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
            guard++
        }
    }

    @Test
    fun `parse accepts valid dates and rejects invalid ones`() {
        assertTrue(Jalali.parseDateToMillis("1403/07/10") != null)
        assertTrue(Jalali.parseDateToMillis("۱۴۰۳/۰۷/۱۰") != null)
        assertTrue(Jalali.parseDateToMillis("1403-7-10") != null)
        assertNull(Jalali.parseDateToMillis("1404/12/30")) // 1404 is not leap
        assertTrue(Jalali.parseDateToMillis("1403/12/30") != null) // 1403 is leap
        assertNull(Jalali.parseDateToMillis("1403/13/01"))
        assertNull(Jalali.parseDateToMillis("1403/07/31"))
        assertNull(Jalali.parseDateToMillis("abc"))
    }

    @Test
    fun `persian digit conversion is symmetric`() {
        assertEquals("۱۲۳", Jalali.faDigits("123"))
        assertEquals("123", Jalali.normalizeDigits("۱۲۳"))
        assertEquals("456", Jalali.normalizeDigits("٤٥٦"))
    }

    @Test
    fun `price formats with persian digits and separators`() {
        assertEquals("۱٬۲۳۴٬۵۶۷", Jalali.price(1_234_567L))
        assertEquals("۰", Jalali.price(0L))
    }
}
