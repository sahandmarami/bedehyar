package ir.bedehyar.app.util

import android.content.Context
import ir.bedehyar.app.App
import ir.bedehyar.app.data.AppDatabase
import ir.bedehyar.app.data.Category
import ir.bedehyar.app.data.DIRECTION_I_OWE
import ir.bedehyar.app.data.DIRECTION_OWES_ME
import ir.bedehyar.app.data.DebtTransaction
import ir.bedehyar.app.data.Payment
import ir.bedehyar.app.data.Person
import ir.bedehyar.app.data.Reminder
import ir.bedehyar.app.data.TxWithRemaining
import org.json.JSONArray
import org.json.JSONObject

/**
 * Offline backup/restore (JSON) and CSV export.
 * All data stays on the device: files are written/read through the
 * Storage Access Framework picks made by the user in the UI.
 */
object BackupManager {

    const val FORMAT_VERSION = 2

    data class ImportSummary(
        val persons: Int,
        val transactions: Int,
        val payments: Int,
        val reminders: Int,
        val categories: Int
    )

    suspend fun exportJson(context: Context): String {
        val db = AppDatabase.get(context)
        val now = System.currentTimeMillis()

        val persons = db.personDao().getAll()
        val categories = db.categoryDao().getAll()
        val txs = db.txDao().queryTransactions(0L, 0L, 0L, 0L, -1)
        val payments = db.paymentDao().inRange(0L, Long.MAX_VALUE, 0L).map { it.payment }
        val reminders = db.reminderDao().allActive(0L) // all active regardless of time

        val root = JSONObject()
        root.put("app", "hesabyar")
        root.put("formatVersion", FORMAT_VERSION)
        root.put("exportedAt", now)

        root.put("persons", JSONArray().apply {
            persons.forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id); put("name", p.name); put("phone", p.phone)
                    put("note", p.note); put("createdAt", p.createdAt)
                })
            }
        })
        root.put("categories", JSONArray().apply {
            categories.forEach { c ->
                put(JSONObject().apply { put("id", c.id); put("name", c.name) })
            }
        })
        root.put("transactions", JSONArray().apply {
            txs.forEach { t ->
                put(JSONObject().apply {
                    put("id", t.tx.id)
                    put("personId", t.tx.personId)
                    put("direction", t.tx.direction)
                    put("amount", t.tx.amount)
                    put("description", t.tx.description)
                    put("categoryId", t.tx.categoryId ?: JSONObject.NULL)
                    put("createdAt", t.tx.createdAt)
                    put("dueDate", t.tx.dueDate ?: JSONObject.NULL)
                    put("reminderHour", t.tx.reminderHour)
                    put("reminderMinute", t.tx.reminderMinute)
                    put("reminderEnabled", t.tx.reminderEnabled)
                    put("isArchived", t.tx.isArchived)
                    put("privateNote", t.tx.privateNote)
                })
            }
        })
        root.put("payments", JSONArray().apply {
            payments.forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id); put("transactionId", p.transactionId)
                    put("amount", p.amount); put("date", p.date); put("note", p.note)
                })
            }
        })
        root.put("reminders", JSONArray().apply {
            reminders.forEach { r ->
                put(JSONObject().apply {
                    put("transactionId", r.transactionId)
                    put("triggerAt", r.triggerAt); put("isActive", r.isActive)
                })
            }
        })
        return root.toString(2)
    }

    /** Validates then REPLACES all data. Throws IllegalArgumentException on bad files. */
    suspend fun importJson(context: Context, json: String): ImportSummary {
        val root = JSONObject(json)
        val appKey = root.optString("app", "")
        val version = root.optInt("formatVersion", -1)
        if (appKey != "hesabyar" || version !in 1..FORMAT_VERSION) {
            throw IllegalArgumentException("فایل پشتیبان معتبر نیست")
        }
        val personsArr = root.getJSONArray("persons")
        val txsArr = root.getJSONArray("transactions")
        val paymentsArr = root.optJSONArray("payments") ?: JSONArray()
        val remindersArr = root.optJSONArray("reminders") ?: JSONArray()
        val categoriesArr = root.optJSONArray("categories") ?: JSONArray()

        val db = AppDatabase.get(context)
        val ledger = (context.applicationContext as App).ledger

        // Wipe (also cancels alarms) then insert with preserved ids.
        ledger.clearEverything()

        val persons = (0 until personsArr.length()).map { i ->
            val o = personsArr.getJSONObject(i)
            Person(
                id = o.getLong("id"),
                name = o.getString("name"),
                phone = o.optString("phone", ""),
                note = o.optString("note", ""),
                createdAt = o.optLong("createdAt", 0L)
            )
        }
        db.personDao().insertAll(persons)

        val categories = (0 until categoriesArr.length()).map { i ->
            val o = categoriesArr.getJSONObject(i)
            Category(id = o.getLong("id"), name = o.getString("name"))
        }
        if (categories.isNotEmpty()) db.categoryDao().insertAll(categories)

        val txs = (0 until txsArr.length()).map { i ->
            val o = txsArr.getJSONObject(i)
            DebtTransaction(
                id = o.getLong("id"),
                personId = o.getLong("personId"),
                direction = o.getInt("direction"),
                amount = o.getLong("amount"),
                description = o.optString("description", ""),
                categoryId = if (o.isNull("categoryId")) null else o.optLong("categoryId"),
                createdAt = o.getLong("createdAt"),
                dueDate = if (o.isNull("dueDate")) null else o.optLong("dueDate"),
                reminderHour = o.optInt("reminderHour", 9),
                reminderMinute = o.optInt("reminderMinute", 0),
                reminderEnabled = o.optBoolean("reminderEnabled", false),
                isArchived = o.optBoolean("isArchived", false),
                privateNote = o.optString("privateNote", "")
            )
        }
        db.txDao().insertAll(txs)

        val payments = (0 until paymentsArr.length()).map { i ->
            val o = paymentsArr.getJSONObject(i)
            Payment(
                id = o.getLong("id"),
                transactionId = o.getLong("transactionId"),
                amount = o.getLong("amount"),
                date = o.optLong("date", 0L),
                note = o.optString("note", "")
            )
        }
        if (payments.isNotEmpty()) db.paymentDao().insertAll(payments)

        val reminders = (0 until remindersArr.length()).map { i ->
            val o = remindersArr.getJSONObject(i)
            Reminder(
                transactionId = o.getLong("transactionId"),
                triggerAt = o.getLong("triggerAt"),
                isActive = o.optBoolean("isActive", true)
            )
        }
        if (reminders.isNotEmpty()) db.reminderDao().insertAll(reminders)

        // Rebuild alarms for still-valid reminders.
        ledger.rebuildAllAlarms()

        return ImportSummary(persons.size, txs.size, payments.size, reminders.size, categories.size)
    }
}

/** CSV export of a transaction list (UTF-8 BOM so Excel opens it correctly). */
object CsvExporter {

    private fun esc(v: String): String =
        "\"" + v.replace("\"", "\"\"") + "\""

    suspend fun transactionsCsv(context: Context, txs: List<TxWithRemaining>): String {
        val db = AppDatabase.get(context)
        val paidSums = HashMap<Long, Long>()
        txs.forEach { paidSums[it.tx.id] = it.paid }

        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.appendLine(
            listOf(
                "شناسه", "شخص", "نوع", "مبلغ", "پرداخت‌شده", "مانده",
                "تاریخ ثبت", "سررسید", "وضعیت", "دسته‌بندی", "توضیحات"
            ).joinToString(",") { esc(it) }
        )
        txs.forEach { t ->
            val typeName = if (t.tx.direction == DIRECTION_I_OWE) "من بدهکارم" else "او به من بدهکار است"
            val status = if (t.isSettled) "تسویه‌شده" else "باز"
            sb.appendLine(
                listOf(
                    t.tx.id.toString(),
                    esc(t.personName),
                    esc(typeName),
                    t.tx.amount.toString(),
                    t.paid.toString(),
                    t.remaining.toString(),
                    esc(Jalali.formatDate(t.tx.createdAt)),
                    esc(t.tx.dueDate?.let { Jalali.formatDate(it) } ?: ""),
                    esc(status),
                    esc(t.categoryName ?: ""),
                    esc(t.tx.description)
                ).joinToString(",")
            )
        }
        return sb.toString()
    }

    fun typeLabel(direction: Int): String =
        if (direction == DIRECTION_I_OWE) "بدهی" else "طلب"
}
