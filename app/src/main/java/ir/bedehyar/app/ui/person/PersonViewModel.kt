package ir.bedehyar.app.ui.person

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.bedehyar.app.App
import ir.bedehyar.app.data.AppDatabase
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
     * ثبت سریع پرداخت جزئی: مبلغ روی قدیمی‌ترین تراکنشِ بازِ این شخص اعمال می‌شود.
     * نتیجه (موفقیت/خطا) از طریق message به Snackbar می‌رود.
     */
    fun recordQuickPayment(amountText: String, note: String) {
        val amount = Fmt.parseAmount(amountText)
        if (amount <= 0L) {
            message.value = "مبلغ پرداخت را وارد کنید"
            return
        }
        viewModelScope.launch {
            val openTxs = db.txDao().observeForPerson(personId).first().filter { !it.isSettled }
            val target = openTxs.minByOrNull { it.tx.createdAt }
            if (target == null) {
                message.value = "همه تراکنش‌های این شخص تسویه شده است"
            } else {
                val err = ledger.addPayment(
                    target.tx.id, amount, System.currentTimeMillis(), note.trim()
                )
                message.value = err ?: "پرداخت " + Fmt.money(amount, false) + " ثبت شد"
            }
        }
    }
}
