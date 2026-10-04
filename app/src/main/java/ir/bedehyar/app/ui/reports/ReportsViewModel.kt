package ir.bedehyar.app.ui.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.bedehyar.app.App
import ir.bedehyar.app.data.AppDatabase
import ir.bedehyar.app.data.DIRECTION_I_OWE
import ir.bedehyar.app.data.DIRECTION_OWES_ME
import ir.bedehyar.app.data.PaymentWithTx
import ir.bedehyar.app.data.ReportTotals
import ir.bedehyar.app.data.TxWithRemaining
import ir.bedehyar.app.util.Jalali
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/* Range types */
const val RANGE_TODAY = 0
const val RANGE_WEEK = 1
const val RANGE_MONTH = 2
const val RANGE_ALL = 3
const val RANGE_CUSTOM = 4

data class DailyPoint(
    val bucketStart: Long,
    val label: String,
    val received: Long,
    val paid: Long
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val now = System.currentTimeMillis()
    private val dayStart = Jalali.startOfToday()

    val rangeType = MutableStateFlow(RANGE_MONTH)
    val customFrom = MutableStateFlow(0L)
    val customTo = MutableStateFlow(0L)
    val personFilter = MutableStateFlow(0L)
    val categoryFilter = MutableStateFlow(0L)
    val listTab = MutableStateFlow(0) // 0 range txs, 1 settled, 2 overdue

    val persons = db.personDao().observeBalances("", now, 0L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val categories = db.categoryDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val range = combine(rangeType, customFrom, customTo) { type, cf, ct ->
        when (type) {
            RANGE_TODAY -> dayStart to (dayStart + 86_399_999L)
            RANGE_WEEK -> {
                // Persian week starts on Saturday.
                val cal = java.util.Calendar.getInstance()
                cal.timeInMillis = dayStart
                val dow = cal.get(java.util.Calendar.DAY_OF_WEEK) // 1=Sun..7=Sat
                val shift = dow % 7 // Sat(7)->0, Sun(1)->1, ..., Fri(6)->6
                (dayStart - shift * 86_400_000L) to (dayStart + 86_399_999L)
            }
            RANGE_MONTH -> {
                val j = Jalali.jalaliOf(dayStart)
                val first = Jalali.millisFromJalali(j[0], j[1], 1, 0, 0)
                first to (dayStart + 86_399_999L)
            }
            RANGE_CUSTOM -> if (cf in 1..ct) cf to (ct + 86_399_999L) else (cf to ct)
            else -> 0L to Long.MAX_VALUE
        }
    }

    private val inputs = combine(range, personFilter, categoryFilter) { r, p, c -> Triple(r, p, c) }

    val totals = inputs.flatMapLatest { (r, p, c) ->
        flow { emit(computeTotals(r.first, r.second, p, c)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportTotals(0, 0, 0, 0, 0, 0))

    val chart = inputs.flatMapLatest { (r, p, _) ->
        flow { emit(computeChart(r.first, r.second, p)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<DailyPoint>())

    val txList = inputs.flatMapLatest { (r, p, c) ->
        flow { emit(db.txDao().queryTransactions(r.first, r.second, p, c, -1)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<TxWithRemaining>())

    val settledList = inputs.flatMapLatest { (r, p, c) ->
        flow { emit(db.txDao().queryTransactions(r.first, r.second, p, c, 1)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<TxWithRemaining>())

    val overdueList = inputs.flatMapLatest { (_, p, c) ->
        flow {
            emit(db.txDao().overdue(now).filter {
                (p == 0L || it.tx.personId == p) && (c == 0L || it.tx.categoryId == c)
            })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<TxWithRemaining>())

    val paymentsInRange = inputs.flatMapLatest { (r, p, _) ->
        flow { emit(db.paymentDao().inRange(r.first, r.second, p)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<PaymentWithTx>())

    private suspend fun computeTotals(from: Long, to: Long, personId: Long, catId: Long): ReportTotals {
        val txs = db.txDao().queryTransactions(from, to, personId, catId, -1)
        val pays = db.paymentDao().inRange(from, to, personId)
        val overdue = db.txDao().overdue(now).filter {
            (personId == 0L || it.tx.personId == personId) && (catId == 0L || it.tx.categoryId == catId)
        }
        return ReportTotals(
            createdCredit = txs.filter { it.tx.direction == DIRECTION_OWES_ME }.sumOf { it.tx.amount },
            createdDebt = txs.filter { it.tx.direction == DIRECTION_I_OWE }.sumOf { it.tx.amount },
            received = pays.filter { it.direction == DIRECTION_OWES_ME }.sumOf { it.payment.amount },
            paid = pays.filter { it.direction == DIRECTION_I_OWE }.sumOf { it.payment.amount },
            settledCount = txs.count { it.isSettled },
            overdueCount = overdue.size
        )
    }

    private suspend fun computeChart(from: Long, to: Long, personId: Long): List<DailyPoint> {
        val pays = db.paymentDao().inRange(from, to, personId)
        if (pays.isEmpty()) return emptyList()
        val dayMs = 86_400_000L
        val spanDays = ((to - from) / dayMs).toInt()

        return if (spanDays <= 31) {
            pays.groupBy { dayStartOf(it.payment.date) }.map { (day, list) ->
                DailyPoint(
                    bucketStart = day,
                    label = Jalali.faDigits(Jalali.jalaliOf(day)[2].toString()),
                    received = list.filter { it.direction == DIRECTION_OWES_ME }.sumOf { it.payment.amount },
                    paid = list.filter { it.direction == DIRECTION_I_OWE }.sumOf { it.payment.amount }
                )
            }.sortedBy { it.bucketStart }
        } else {
            // monthly buckets (jalali month)
            pays.groupBy { monthStartOf(it.payment.date) }.map { (mStart, list) ->
                val j = Jalali.jalaliOf(mStart)
                DailyPoint(
                    bucketStart = mStart,
                    label = Jalali.faDigits("${j[0]}/${j[1]}".replace("/", "/")),
                    received = list.filter { it.direction == DIRECTION_OWES_ME }.sumOf { it.payment.amount },
                    paid = list.filter { it.direction == DIRECTION_I_OWE }.sumOf { it.payment.amount }
                )
            }.sortedBy { it.bucketStart }
        }
    }

    private fun dayStartOf(millis: Long): Long {
        val j = Jalali.jalaliOf(millis)
        return Jalali.millisFromJalali(j[0], j[1], j[2], 0, 0)
    }

    private fun monthStartOf(millis: Long): Long {
        val j = Jalali.jalaliOf(millis)
        val g = Jalali.toGregorian(j[0], j[1], 1)
        val cal = java.util.Calendar.getInstance()
        cal.clear()
        cal.set(g[0], g[1] - 1, g[2], 0, 0, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
