package ir.bedehyar.app.ui.tx

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.bedehyar.app.App
import ir.bedehyar.app.data.AppDatabase
import ir.bedehyar.app.data.Category
import ir.bedehyar.app.data.DIRECTION_I_OWE
import ir.bedehyar.app.data.DIRECTION_OWES_ME
import ir.bedehyar.app.data.DebtTransaction
import ir.bedehyar.app.data.Person
import ir.bedehyar.app.util.Fmt
import ir.bedehyar.app.util.Jalali
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EditViewModel(
    app: Application,
    private val txId: Long,
    private val presetPersonId: Long,
    private val presetType: Int
) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val ledger = (app as App).ledger
    private val settings = (app as App).settings

    val persons = db.personDao().observeBalances("", System.currentTimeMillis(), 0L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val categories = db.categoryDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /* form state */
    val name = MutableStateFlow("")
    val phone = MutableStateFlow("")
    val amountText = MutableStateFlow("")
    val direction = MutableStateFlow(if (presetType == DIRECTION_OWES_ME) DIRECTION_OWES_ME else DIRECTION_I_OWE)
    val description = MutableStateFlow("")
    val categoryId = MutableStateFlow(0L) // 0 = بدون دسته
    val registerDate = MutableStateFlow(System.currentTimeMillis())
    val hasDue = MutableStateFlow(false)
    val dueDate = MutableStateFlow(0L)
    val reminderEnabled = MutableStateFlow(false)
    val reminderHour = MutableStateFlow(settings_snapshot_hour())
    val reminderMinute = MutableStateFlow(0)
    val privateNote = MutableStateFlow("")

    val isEdit = txId > 0L
    var loadedPersonId: Long = presetPersonId
    var loadedPersonName: String = ""

    val isSaving = MutableStateFlow(false)
    val nameError = MutableStateFlow<String?>(null)
    val amountError = MutableStateFlow<String?>(null)
    val dueError = MutableStateFlow<String?>(null)

    private fun settings_snapshot_hour() = 9

    init {
        viewModelScope.launch {
            val s = settings.snapshot()
            reminderHour.value = s.defaultReminderHour
            reminderMinute.value = s.defaultReminderMinute
        }
        if (isEdit) {
            viewModelScope.launch {
                val tw = db.txDao().getById(txId) ?: return@launch
                loadedPersonId = tw.tx.personId
                loadedPersonName = tw.personName
                name.value = tw.personName
                amountText.value = Fmt.groupDigits(tw.tx.amount.toString())
                direction.value = tw.tx.direction
                description.value = tw.tx.description
                categoryId.value = tw.tx.categoryId ?: 0L
                registerDate.value = tw.tx.createdAt
                hasDue.value = tw.tx.dueDate != null
                dueDate.value = tw.tx.dueDate ?: 0L
                reminderEnabled.value = tw.tx.reminderEnabled
                reminderHour.value = tw.tx.reminderHour
                reminderMinute.value = tw.tx.reminderMinute
                privateNote.value = tw.tx.privateNote
            }
        } else if (presetPersonId > 0L) {
            viewModelScope.launch {
                val p = db.personDao().getById(presetPersonId)
                if (p != null) {
                    loadedPersonId = p.id
                    loadedPersonName = p.name
                    name.value = p.name
                }
            }
        }
    }

    private fun normalizedMidnight(millis: Long): Long {
        val j = Jalali.jalaliOf(millis)
        return Jalali.millisFromJalali(j[0], j[1], j[2], 0, 0)
    }

    /**
     * Validates and saves. Returns false and sets error states when invalid.
     * Guarded against double-tap with [isSaving].
     */
    fun save(onSuccess: () -> Unit) {
        if (isSaving.value) return
        nameError.value = null
        amountError.value = null
        dueError.value = null

        val personName = name.value.trim()
        val amount = Fmt.parseAmount(amountText.value)
        if (personName.isEmpty()) {
            nameError.value = "نام شخص اجباری است"
            return
        }
        if (amount <= 0L) {
            amountError.value = "مبلغ باید بزرگ‌تر از صفر باشد"
            return
        }
        if (hasDue.value && dueDate.value <= 0L) {
            dueError.value = "تاریخ سررسید را انتخاب کنید"
            return
        }
        if (hasDue.value && reminderEnabled.value) {
            val j = Jalali.jalaliOf(dueDate.value)
            val trigger = Jalali.millisFromJalali(j[0], j[1], j[2], reminderHour.value, reminderMinute.value)
            if (trigger <= System.currentTimeMillis()) {
                dueError.value = "زمان یادآوری باید در آینده باشد"
                return
            }
        }

        isSaving.value = true
        viewModelScope.launch {
            try {
                val pid = if (isEdit) {
                    loadedPersonId
                } else if (loadedPersonId > 0L && name.value.trim() == loadedPersonName) {
                    loadedPersonId
                } else {
                    val existing = db.personDao().findByName(personName)
                    if (existing != null) {
                        existing.id
                    } else {
                        db.personDao().insert(
                            Person(name = personName, phone = phone.value.trim())
                        )
                    }
                }

                val tx = DebtTransaction(
                    id = if (isEdit) txId else 0L,
                    personId = pid,
                    direction = direction.value,
                    amount = amount,
                    description = description.value.trim(),
                    categoryId = categoryId.value.takeIf { it > 0L },
                    createdAt = normalizedMidnight(registerDate.value),
                    dueDate = if (hasDue.value) normalizedMidnight(dueDate.value) else null,
                    reminderHour = reminderHour.value,
                    reminderMinute = reminderMinute.value,
                    reminderEnabled = hasDue.value && reminderEnabled.value,
                    isArchived = false,
                    privateNote = privateNote.value.trim()
                )
                ledger.saveTransaction(tx)
                onSuccess()
            } catch (e: Exception) {
                amountError.value = "ذخیره‌سازی ناموفق بود: ${e.message}"
            } finally {
                isSaving.value = false
            }
        }
    }
}
