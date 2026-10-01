package ir.bedehyar.app.util

/**
 * Pure balance/payment math — kept free of Android dependencies so it is
 * unit-testable on the JVM.
 *
 * Example (from the product spec):
 *   amount = 5,000,000 ; paid = 2,000,000  =>  remaining = 3,000,000
 */
object PaymentMath {

    fun remaining(amount: Long, paid: Long): Long = (amount - paid).coerceAtLeast(0L)

    fun isSettled(amount: Long, paid: Long): Boolean = paid >= amount

    /**
     * Validates a new/updated payment against the transaction amount.
     * Returns null when valid, otherwise a Persian error message.
     */
    fun validatePayment(amount: Long, paidSoFar: Long, payment: Long): String? = when {
        payment <= 0L -> "مبلغ پرداخت باید بزرگ‌تر از صفر باشد"
        paidSoFar >= amount -> "این تراکنش قبلاً تسویه شده است"
        paidSoFar + payment > amount -> "جمع پرداخت‌ها نمی‌تواند از مبلغ تراکنش بیشتر شود"
        else -> null
    }
}
