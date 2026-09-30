package ir.bedehyar.app

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.bedehyar.app.alarm.AlarmScheduler
import ir.bedehyar.app.data.AppDatabase
import ir.bedehyar.app.data.DebtTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs

class TxViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).dao()
    private val prefs = app.getSharedPreferences("bedehyar_prefs", Context.MODE_PRIVATE)

    var onboardingDone by mutableStateOf(prefs.getBoolean(KEY_ONBOARDING, false))
        private set

    val allTx: StateFlow<List<DebtTransaction>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    data class Summary(val debt: Long, val credit: Long) {
        val net: Long get() = credit - debt
    }

    val summary: StateFlow<Summary> = allTx.map { list ->
        val debt = list.filter { it.iOwe && !it.isSettled }.sumOf { it.remaining }
        val credit = list.filter { !it.iOwe && !it.isSettled }.sumOf { it.remaining }
        Summary(debt, credit)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Summary(0, 0))

    data class PersonSummary(
        val name: String,
        val credit: Long,   // او به من بدهکار است (طلب)
        val debt: Long,     // من به او بدهکارم
        val openCount: Int
    ) {
        val net: Long get() = credit - debt
    }

    val persons: StateFlow<List<PersonSummary>> = allTx.map { list ->
        list.filter { !it.isSettled }
            .groupBy { it.personName }
            .map { (name, txs) ->
                PersonSummary(
                    name = name,
                    credit = txs.filter { !it.iOwe }.sumOf { it.remaining },
                    debt = txs.filter { it.iOwe }.sumOf { it.remaining },
                    openCount = txs.size
                )
            }
            .sortedByDescending { abs(it.net) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun txById(id: Long): DebtTransaction? = allTx.value.firstOrNull { it.id == id }

    fun txsForFlow(name: String): Flow<List<DebtTransaction>> =
        allTx.map { list -> list.filter { it.personName == name } }

    fun completeOnboarding() {
        prefs.edit().putBoolean(KEY_ONBOARDING, true).apply()
        onboardingDone = true
    }

    fun save(
        id: Long,
        name: String,
        amount: Long,
        iOwe: Boolean,
        note: String,
        dueAt: Long?,
        alarm: Boolean
    ) = viewModelScope.launch {
        val ctx = getApplication<Application>()
        val now = System.currentTimeMillis()
        if (id == -1L) {
            val tx = DebtTransaction(
                personName = name.trim(),
                amount = amount,
                iOwe = iOwe,
                note = note.trim(),
                dueAt = dueAt,
                alarmEnabled = alarm
            )
            val newId = dao.insert(tx)
            if (alarm && dueAt != null && dueAt > now) {
                AlarmScheduler.schedule(ctx, tx.copy(id = newId))
            }
        } else {
            val old = dao.getById(id) ?: return@launch
            val tx = old.copy(
                personName = name.trim(),
                amount = amount,
                iOwe = iOwe,
                note = note.trim(),
                dueAt = dueAt,
                alarmEnabled = alarm
            )
            AlarmScheduler.cancel(ctx, id)
            dao.update(tx)
            if (alarm && dueAt != null && dueAt > now && !tx.isSettled) {
                AlarmScheduler.schedule(ctx, tx)
            }
        }
    }

    fun delete(tx: DebtTransaction) = viewModelScope.launch {
        AlarmScheduler.cancel(getApplication(), tx.id)
        dao.delete(tx)
    }

    fun pay(tx: DebtTransaction, payAmount: Long) = viewModelScope.launch {
        if (payAmount <= 0) return@launch
        val newPaid = (tx.paidAmount + payAmount).coerceAtMost(tx.amount)
        val updated = tx.copy(paidAmount = newPaid)
        dao.update(updated)
        if (updated.isSettled) {
            AlarmScheduler.cancel(getApplication(), tx.id)
        }
    }

    companion object {
        private const val KEY_ONBOARDING = "onboarding_done"
    }
}
