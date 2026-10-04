package ir.bedehyar.app.ui.tx

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.bedehyar.app.App
import ir.bedehyar.app.data.AppDatabase
import ir.bedehyar.app.data.Payment
import ir.bedehyar.app.data.TxWithRemaining
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@kotlinx.coroutines.ExperimentalCoroutinesApi
class TxDetailViewModel(app: Application, private val txId: Long) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val ledger = (app as App).ledger

    val tx = db.txDao().observeById(txId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val payments = db.paymentDao().observeForTx(txId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val message = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)

    fun consumeMessage() { message.value = null }

    /** Adds a payment; returns true when accepted. */
    fun addPayment(amount: Long, date: Long, note: String, onDone: (Boolean) -> Unit) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            val err = ledger.addPayment(txId, amount, date, note)
            busy.value = false
            if (err != null) {
                message.value = err
                onDone(false)
            } else {
                message.value = "پرداخت ثبت شد"
                onDone(true)
            }
        }
    }

    fun settleFull(date: Long, onDone: (Boolean) -> Unit) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            val err = ledger.settleFull(txId, date)
            busy.value = false
            if (err != null) {
                message.value = err
                onDone(false)
            } else {
                message.value = "تراکنش به‌طور کامل تسویه شد و یادآوری آن لغو گردید"
                onDone(true)
            }
        }
    }

    fun updatePayment(payment: Payment, onDone: (Boolean) -> Unit) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            val err = ledger.updatePayment(payment)
            busy.value = false
            if (err != null) {
                message.value = err; onDone(false)
            } else {
                message.value = "پرداخت ویرایش شد"; onDone(true)
            }
        }
    }

    fun deletePayment(payment: Payment, onDone: (Boolean) -> Unit) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            ledger.deletePayment(payment)
            busy.value = false
            message.value = "پرداخت حذف شد"
            onDone(true)
        }
    }

    fun deleteTransaction(onDone: () -> Unit) {
        viewModelScope.launch {
            ledger.deleteTransaction(txId)
            onDone()
        }
    }
}
