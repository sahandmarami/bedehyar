package ir.bedehyar.app.data

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

/**
 * A single debt/credit transaction.
 * iOwe == true  -> "من بدهکارم" (I owe this person)
 * iOwe == false -> "او بدهکار است" (this person owes me / طلب من)
 */
@Entity(tableName = "transactions")
data class DebtTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val personName: String,
    val amount: Long,
    val paidAmount: Long = 0L,
    val iOwe: Boolean,
    val note: String = "",
    val dueAt: Long? = null,
    val alarmEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    @Ignore
    val isSettled: Boolean = paidAmount >= amount

    @Ignore
    val remaining: Long = (amount - paidAmount).coerceAtLeast(0L)
}
