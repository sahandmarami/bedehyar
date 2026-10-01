package ir.bedehyar.app.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/*
 * Account direction conventions:
 *  DIRECTION_I_OWE   = 0  -> «من بدهکارم»  (I owe this person money)
 *  DIRECTION_OWES_ME = 1  -> «او به من بدهکار است» (this person owes me)
 * Amounts are stored as whole Toman (Long). Currency display unit
 * (Toman/Rial) is a presentation-only setting.
 */

const val DIRECTION_I_OWE = 0
const val DIRECTION_OWES_ME = 1

@Entity(tableName = "persons")
data class Person(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val phone: String = "",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Person::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("personId"), Index("dueDate"), Index("isArchived")]
)
data class DebtTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val personId: Long,
    val direction: Int,
    val amount: Long,
    val description: String = "",
    val categoryId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val reminderEnabled: Boolean = false,
    val isArchived: Boolean = false,
    val privateNote: String = ""
)

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = DebtTransaction::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("transactionId")]
)
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val transactionId: Long,
    val amount: Long,
    val date: Long = System.currentTimeMillis(),
    val note: String = ""
)

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = DebtTransaction::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["transactionId"], unique = true)]
)
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val transactionId: Long,
    val triggerAt: Long,
    val isActive: Boolean = true
)

@Entity(tableName = "categories", indices = [Index(value = ["name"], unique = true)])
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String
)

/* ------------------------------------------------------------------ */
/* Query result POJOs                                                  */
/* ------------------------------------------------------------------ */

/** One transaction plus its paid amount, owner name and category name. */
data class TxWithRemaining(
    @Embedded val tx: DebtTransaction,
    val paid: Long,
    val personName: String,
    val categoryName: String?
) {
    val remaining: Long get() = (tx.amount - paid).coerceAtLeast(0L)
    val isSettled: Boolean get() = remaining <= 0L
}

/** Per-person aggregated balance over all non-archived transactions. */
data class PersonWithBalance(
    @Embedded val person: Person,
    val creditRemaining: Long,   // مانده طلب من از این شخص
    val debtRemaining: Long,     // مانده بدهی من به این شخص
    val openCount: Int,          // تعداد تراکنش‌های باز
    val overdueCount: Int,       // تعداد سررسیدهای گذشته
    val lastActivity: Long
) {
    val net: Long get() = creditRemaining - debtRemaining
    val hasOpenAccount: Boolean get() = openCount > 0
}

/** Global dashboard statistics (single row). */
data class DashboardStats(
    val totalCredit: Long,
    val totalDebt: Long,
    val openPersons: Int,
    val dueToday: Int,
    val overdue: Int,
    val nextDueDate: Long?
) {
    val net: Long get() = totalCredit - totalDebt
}

/** A payment joined with its transaction direction and person name. */
data class PaymentWithTx(
    @Embedded val payment: Payment,
    val direction: Int,
    val personName: String
)

/** Aggregate report numbers for a date range. */
data class ReportTotals(
    val createdCredit: Long,   // طلب‌های ثبت‌شده در بازه
    val createdDebt: Long,     // بدهی‌های ثبت‌شده در بازه
    val received: Long,        // مجموع دریافت‌ها (پرداخت روی طلب‌ها)
    val paid: Long,            // مجموع پرداخت‌ها (پرداخت روی بدهی‌ها)
    val settledCount: Int,     // تراکنش‌های تسویه‌شده در بازه
    val overdueCount: Int      // سررسیدهای گذشته
)
