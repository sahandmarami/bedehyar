package ir.bedehyar.app.data

import android.content.Context
import ir.bedehyar.app.alarm.AlarmScheduler

/**
 * Financial ledger operations with database-integrity guarantees.
 *
 * Every mutation keeps transactions, payments, reminders and the system
 * AlarmManager in a consistent state:
 *  - full settlement archives the transaction and cancels its alarm
 *  - partial payments never change the original due date
 *  - deleting/reducing a payment re-opens an archived transaction
 *  - all money amounts are Long Toman; the balance math lives in PaymentMath
 */
class Ledger(
    private val context: Context,
    private val db: AppDatabase,
    private val settings: SettingsRepository
) {

    /* ------------------------- transactions ------------------------- */

    suspend fun saveTransaction(tx: DebtTransaction): Long {
        val id = if (tx.id == 0L) {
            db.txDao().insert(tx)
        } else {
            db.txDao().update(tx)
            tx.id
        }
        rescheduleAlarm(id)
        return id
    }

    suspend fun deleteTransaction(txId: Long) {
        AlarmScheduler.cancel(context, txId)
        db.reminderDao().deleteForTx(txId)
        val tw = db.txDao().getById(txId) ?: return
        db.txDao().delete(tw.tx)
    }

    /** Used by person editing (cascade-free rename/phone update). */
    suspend fun savePerson(person: Person): Long {
        return if (person.id == 0L) {
            db.personDao().insert(person)
        } else {
            db.personDao().update(person)
            person.id
        }
    }

    suspend fun deletePerson(personId: Long) {
        val txs = db.txDao().queryTransactions(0L, 0L, personId, 0L, -1)
        txs.forEach { AlarmScheduler.cancel(context, it.tx.id) }
        db.personDao().delete(db.personDao().getById(personId) ?: return)
    }

    /* -------------------------- payments ---------------------------- */

    /**
     * Adds a payment. Returns null on success or a Persian error message.
     * A payment may never exceed the remaining balance of its transaction.
     */
    suspend fun addPayment(txId: Long, amount: Long, date: Long, note: String): String? {
        if (amount <= 0L) return "مبلغ پرداخت باید بزرگ‌تر از صفر باشد"
        val tw = db.txDao().getById(txId) ?: return "تراکنش یافت نشد"
        if (tw.remaining <= 0L) return "این تراکنش قبلاً تسویه شده است"
        if (amount > tw.remaining) return "مبلغ پرداخت نمی‌تواند بیشتر از مانده (${tw.remaining}) باشد"
        db.paymentDao().insert(Payment(transactionId = txId, amount = amount, date = date, note = note))
        reconcile(txId)
        return null
    }

    /** Convenience: pay the whole remaining balance at once. */
    suspend fun settleFull(txId: Long, date: Long): String? {
        val remaining = db.txDao().getById(txId)?.remaining ?: return "تراکنش یافت نشد"
        return addPayment(txId, remaining, date, "تسویه کامل")
    }

    suspend fun updatePayment(payment: Payment): String? {
        val tw = db.txDao().getById(payment.transactionId) ?: return "تراکنش یافت نشد"
        if (payment.amount <= 0L) return "مبلغ پرداخت باید بزرگ‌تر از صفر باشد"
        val others = db.paymentDao().sumForExcept(payment.transactionId, payment.id)
        if (others + payment.amount > tw.tx.amount) {
            return "جمع پرداخت‌ها نمی‌تواند از مبلغ تراکنش بیشتر شود"
        }
        db.paymentDao().update(payment)
        reconcile(payment.transactionId)
        return null
    }

    suspend fun deletePayment(payment: Payment) {
        db.paymentDao().delete(payment)
        reconcile(payment.transactionId)
    }

    /**
     * Recomputes archived state and alarm schedule after any payment change.
     * Settled -> archive + cancel alarm. Re-opened -> unarchive (past reminders
     * stay deactivated to avoid firing stale alarms).
     */
    private suspend fun reconcile(txId: Long) {
        val tw = db.txDao().getById(txId) ?: return
        val settled = tw.remaining <= 0L
        if (settled) {
            if (!tw.tx.isArchived) db.txDao().setArchived(txId, true)
            AlarmScheduler.cancel(context, txId)
            db.reminderDao().deactivate(txId)
        } else if (tw.tx.isArchived) {
            db.txDao().setArchived(txId, false)
        }
    }

    /** (Re)schedules the reminder alarm of a transaction according to current settings. */
    suspend fun rescheduleAlarm(txId: Long) {
        val tw = db.txDao().getById(txId) ?: return
        val master = settings.snapshot().remindersEnabled
        val tx = tw.tx
        if (master && tx.reminderEnabled && tx.dueDate != null && !tw.isSettled && !tx.isArchived) {
            AlarmScheduler.schedule(context, tx, master)
        } else {
            AlarmScheduler.cancel(context, txId)
            db.reminderDao().deactivate(txId)
        }
    }

    /* -------------------------- categories -------------------------- */

    suspend fun ensureCategories() {
        if (db.categoryDao().count() == 0) {
            db.categoryDao().insertAll(AppDatabase.DEFAULT_CATEGORIES.map { Category(name = it) })
        }
    }

    /* ------------------------- backup/restore ----------------------- */

    suspend fun clearEverything() {
        AlarmScheduler.cancelAll(context, db)
        db.reminderDao().deleteAll()
        db.paymentDao().deleteAll()
        db.txDao().deleteAll()
        db.personDao().deleteAll()
        db.categoryDao().deleteAll()
    }

    /** Rebuilds every still-valid reminder alarm after a restore or boot. */
    suspend fun rebuildAllAlarms() {
        val master = settings.snapshot().remindersEnabled
        val now = System.currentTimeMillis()
        db.reminderDao().allActive(now).forEach { reminder ->
            val tw = db.txDao().getById(reminder.transactionId) ?: return@forEach
            if (master && tw.tx.reminderEnabled && !tw.isSettled && !tw.tx.isArchived) {
                AlarmScheduler.scheduleAt(context, reminder.transactionId, reminder.triggerAt)
            } else {
                db.reminderDao().deactivate(reminder.transactionId)
            }
        }
    }
}
