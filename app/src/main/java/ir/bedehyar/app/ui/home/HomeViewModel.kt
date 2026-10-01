package ir.bedehyar.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.bedehyar.app.App
import ir.bedehyar.app.data.AppDatabase
import ir.bedehyar.app.data.PersonWithBalance
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/* Filters */
const val FILTER_ALL = 0
const val FILTER_DEBTORS = 1
const val FILTER_CREDITORS = 2
const val FILTER_SETTLED = 3
const val FILTER_OVERDUE = 4

/* Sorts */
const val SORT_NAME = 0
const val SORT_AMOUNT = 1
const val SORT_DATE = 2

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val settings = (app as App).settings

    /** Anchored once per VM creation; queries stay coherent within a session. */
    private val now = System.currentTimeMillis()
    private val dayStart = ir.bedehyar.app.util.Jalali.startOfToday()
    private val dayEnd = dayStart + 86_400_000L

    val search = MutableStateFlow("")
    val filter = MutableStateFlow(FILTER_ALL)
    val sort = MutableStateFlow(SORT_NAME)

    val stats = db.txDao().observeStats(dayStart, dayEnd, now)
        .map { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val persons = combine(search.debounce(200), filter, sort) { q, f, s -> Triple(q, f, s) }
        .flatMapLatest { (q, f, s) ->
            db.personDao().observeBalances(q.trim(), now, 0L).map { list ->
                applyFilterSort(list, f, s)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun applyFilterSort(
        list: List<PersonWithBalance>,
        filterId: Int,
        sortId: Int
    ): List<PersonWithBalance> {
        val filtered = when (filterId) {
            FILTER_DEBTORS -> list.filter { it.debtRemaining > 0 }
            FILTER_CREDITORS -> list.filter { it.creditRemaining > 0 }
            FILTER_SETTLED -> list.filter { !it.hasOpenAccount }
            FILTER_OVERDUE -> list.filter { it.overdueCount > 0 }
            else -> list
        }
        return when (sortId) {
            SORT_AMOUNT -> filtered.sortedByDescending { kotlin.math.abs(it.net) }
            SORT_DATE -> filtered.sortedByDescending { it.lastActivity }
            else -> filtered // DB already ordered by name
        }
    }
}
