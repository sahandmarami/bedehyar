package ir.bedehyar.app

import ir.bedehyar.app.util.Fmt
import ir.bedehyar.app.util.PaymentMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentMathTest {

    /** Spec example: 5,000,000 with 2,000,000 paid -> 3,000,000 remaining. */
    @Test
    fun `spec example computes remaining correctly`() {
        val remaining = PaymentMath.remaining(5_000_000L, 2_000_000L)
        assertEquals(3_000_000L, remaining)
        assertFalse(PaymentMath.isSettled(5_000_000L, 2_000_000L))
    }

    @Test
    fun `full payment settles the transaction`() {
        assertTrue(PaymentMath.isSettled(1_000_000L, 1_000_000L))
        assertEquals(0L, PaymentMath.remaining(1_000_000L, 1_500_000L))
    }

    @Test
    fun `overpayment is rejected`() {
        val err = PaymentMath.validatePayment(1_000_000L, 800_000L, 300_000L)
        assertNotNull(err)
    }

    @Test
    fun `zero and negative payments are rejected`() {
        assertNotNull(PaymentMath.validatePayment(1_000_000L, 0L, 0L))
        assertNotNull(PaymentMath.validatePayment(1_000_000L, 0L, -5L))
    }

    @Test
    fun `valid partial payment passes`() {
        assertNull(PaymentMath.validatePayment(1_000_000L, 300_000L, 400_000L))
    }

    @Test
    fun `multiple partial payments sum up to the amount`() {
        var paid = 0L
        val parts = listOf(2_000_000L, 1_500_000L, 1_500_000L)
        parts.forEach { p ->
            assertNull(PaymentMath.validatePayment(5_000_000L, paid, p))
            paid += p
        }
        assertTrue(PaymentMath.isSettled(5_000_000L, paid))
        assertEquals(0L, PaymentMath.remaining(5_000_000L, paid))
    }
}

class FmtTest {

    @Test
    fun `parseAmount handles persian digits and separators`() {
        assertEquals(1234567L, Fmt.parseAmount("۱٬۲۳۴٬۵۶۷"))
        assertEquals(1234567L, Fmt.parseAmount("1,234,567"))
        assertEquals(5000L, Fmt.parseAmount("۵۰۰۰"))
        assertEquals(0L, Fmt.parseAmount(""))
    }

    @Test
    fun `groupDigits inserts thousand separators`() {
        assertEquals("1,234,567", Fmt.groupDigits("1234567"))
        assertEquals("", Fmt.groupDigits(""))
    }

    @Test
    fun `money multiplies by ten for rial display only`() {
        val toman = Fmt.money(1000L, rial = false)
        val rial = Fmt.money(1000L, rial = true)
        assertTrue(toman.contains("۱٬۰۰۰"))
        assertTrue(rial.contains("۱۰٬۰۰۰"))
        assertTrue(toman.endsWith("تومان"))
        assertTrue(rial.endsWith("ریال"))
    }
}
