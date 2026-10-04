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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class PersonViewModel(app: Application, private val personId: Long) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val ledger = (app as App).ledger

    private val now = System.currentTimeMillis()

    val person = db.personDao().observeById(personId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val balance = db.personDao().observeBalances("", now, personId)
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /* transaction tab filters: status 0=all, 1=open, 2=settled */
    val status = MutableStateFlow(0)
    val from = MutableStateFlow(0L)
    val to = MutableStateFlow(0L)
    val tab = MutableStateFlow(0) // 0 = transactions, 1 = payments

    val transactions = combine(status, from, to) { s, f, t -> Triple(s, f, t) }
        .flatMapLatest { (s, f, t) ->
            db.txDao().observeForPerson(personId).map { list ->
                list.filter { item ->
                    val statusOk = when (s) {
                        1 -> !item.isSettled
                        2 -> item.isSettled
                        else -> true
                    }
                    val fromOk = f == 0L || item.tx.createdAt >= f
                    val toOk = t == 0L || item.tx.createdAt <= (t + 86_399_999L)
                    statusOk && fromOk && toOk
                }
            }
        }
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
}
