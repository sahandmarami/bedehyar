package ir.bedehyar.app.ui.person

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.bedehyar.app.App
import ir.bedehyar.app.data.AppDatabase
import ir.bedehyar.app.data.DIRECTION_I_OWE
import ir.bedehyar.app.data.DIRECTION_OWES_ME
import ir.bedehyar.app.data.PaymentWithTx
import ir.bedehyar.app.data.Person
import ir.bedehyar.app.data.PersonWithBalance
import ir.bedehyar.app.data.TxWithRemaining
import ir.bedehyar.app.util.Fmt
import ir.bedehyar.app.util.Jalali
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PersonViewModel(app: Application, private val personId: Long) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val ledger = (app as App).ledger

    private val now = System.currentTimeMillis()

    val person = db.personDao().observeById(personId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val balance = db.personDao().observeBalances("", now, personId)
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /* همه تراکنش‌ها و همه پرداخت‌ها، بدون هیچ فیلتری (لیست ساده) */
    val transactions = db.txDao().observeForPerson(personId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val payments = db.paymentDao().observeForPerson(personId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<PaymentWithTx>())

    /* person editing */
    val busy = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)

    fun savePerson(name: String, phone: String, note: String, onDone: () -> Unit) {
        if (busy.value) return
        if (name.isBlank()) {
            message.value = "نام شخص نمی‌تواند خالی باشد"
            return
        }
        busy.value = true
        viewModelScope.launch {
            val current = db.personDao().getById(personId)
            if (current != null) {
                ledger.savePerson(current.copy(name = name.trim(), phone = phone.trim(), note = note.trim()))
                message.value = "اطلاعات شخص ذخیره شد"
            }
            busy.value = false
            onDone()
        }
    }

    fun deletePerson(onDone: () -> Unit) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            ledger.deletePerson(personId)
            busy.value = false
            onDone()
        }
    }

    fun consumeMessage() { message.value = null }

    /**
     * ثبت سریع پرداخت: مبلغ از «مجموعِ» تراکنش‌های بازِ این شخص کم می‌شود و
     * خودکار بین آنها تخصیص می‌یابد (قدیمی‌ترین اول)، پس بزرگ‌تربودن مبلغ از
     * ماندهِ یک تراکنش دیگر خطا نمی‌دهد. اول تراکنش‌های «او به من بدهکار»
     * (دریافت از او) و سپس «من به او بدهکار» پر می‌شوند. اگر مبلغ از ماندهِ کل
     * بیشتر باشد همه تراکنش‌ها تسویه و مازاد در پیام اعلام می‌شود.
     * نتیجه از طریق message به Snackbar می‌رود.
     */
    fun recordQuickPayment(amountText: String, note: String) {
        val amount = Fmt.parseAmount(amountText)
        if (amount <= 0L) {
            message.value = "مبلغ پرداخت را وارد کنید"
            return
        }
        viewModelScope.launch {
            val openTxs = db.txDao().observeForPerson(personId).first().filter { !it.isSettled }
            if (openTxs.isEmpty()) {
                message.value = "همه تراکنش‌های این شخص تسویه شده است"
                return@launch
            }
            val ordered =
                openTxs.filter { it.tx.direction == DIRECTION_OWES_ME }.sortedBy { it.tx.createdAt } +
                openTxs.filter { it.tx.direction == DIRECTION_I_OWE }.sortedBy { it.tx.createdAt }
            val totalRemaining = ordered.sumOf { it.remaining }
            val applicable = minOf(amount, totalRemaining)
            val date = System.currentTimeMillis()
            var left = applicable
            var touched = 0
            var failed: String? = null
            for (t in ordered) {
                if (left <= 0L) break
                val pay = minOf(left, t.remaining)
                val err = ledger.addPayment(t.tx.id, pay, date, note.trim())
                if (err != null) { failed = err; break }
                left -= pay
                touched++
            }
            message.value = when {
                failed != null -> failed
                amount > totalRemaining ->
                    "همه تراکنش‌ها تسویه شد؛ ${Fmt.money(amount - totalRemaining, false)} بیشتر از مانده بود و اضافه آن ثبت نشد"
                touched > 1 ->
                    "پرداخت ${Fmt.money(applicable, false)} بین $touched تراکنش تخصیص یافت"
                else ->
                    "پرداخت ${Fmt.money(applicable, false)} ثبت شد"
            }
        }
    }
}
