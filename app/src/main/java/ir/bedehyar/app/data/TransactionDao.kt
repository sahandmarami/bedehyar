package ir.bedehyar.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DebtTransaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): DebtTransaction?

    @Query(
        "SELECT * FROM transactions WHERE alarmEnabled = 1 AND dueAt IS NOT NULL " +
            "AND dueAt > :now AND paidAmount < amount"
    )
    suspend fun pendingAlarms(now: Long): List<DebtTransaction>

    @Insert
    suspend fun insert(tx: DebtTransaction): Long

    @Update
    suspend fun update(tx: DebtTransaction)

    @Delete
    suspend fun delete(tx: DebtTransaction)
}
