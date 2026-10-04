package ir.bedehyar.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/*
 * Shared SELECT fragment: transaction columns + paid sum + person/category names.
 * Kept inline in each query (Room does not allow dynamic fragments).
 */
private const val TX_SELECT =
    "SELECT t.*, COALESCE(pay.paid, 0) AS paid, p.name AS personName, c.name AS categoryName " +
        "FROM transactions t " +
        "JOIN persons p ON p.id = t.personId " +
        "LEFT JOIN categories c ON c.id = t.categoryId " +
        "LEFT JOIN (SELECT transactionId, SUM(amount) AS paid FROM payments GROUP BY transactionId) pay " +
        "ON pay.transactionId = t.id"

@Dao
interface PersonDao {

    @Query(
        "SELECT p.*, " +
            "COALESCE(SUM(CASE WHEN t.direction = 1 AND t.amount - COALESCE(pay.paid, 0) > 0 " +
            "THEN t.amount - COALESCE(pay.paid, 0) ELSE 0 END), 0) AS creditRemaining, " +
            "COALESCE(SUM(CASE WHEN t.direction = 0 AND t.amount - COALESCE(pay.paid, 0) > 0 " +
            "THEN t.amount - COALESCE(pay.paid, 0) ELSE 0 END), 0) AS debtRemaining, " +
            "COUNT(CASE WHEN t.id IS NOT NULL AND t.amount - COALESCE(pay.paid, 0) > 0 THEN 1 END) AS openCount, " +
            "COUNT(CASE WHEN t.id IS NOT NULL AND t.dueDate IS NOT NULL AND t.dueDate < :now " +
            "AND t.amount - COALESCE(pay.paid, 0) > 0 THEN 1 END) AS overdueCount, " +
            "COALESCE(MAX(t.createdAt), p.createdAt) AS lastActivity " +
            "FROM persons p " +
            "LEFT JOIN transactions t ON t.personId = p.id AND t.isArchived = 0 " +
            "LEFT JOIN (SELECT transactionId, SUM(amount) AS paid FROM payments GROUP BY transactionId) pay " +
            "ON pay.transactionId = t.id " +
            "WHERE (:personId = 0 OR p.id = :personId) AND (:q = '' OR p.name LIKE '%' || :q || '%') " +
            "GROUP BY p.id " +
            "ORDER BY p.name COLLATE NOCASE"
    )
    fun observeBalances(q: String, now: Long, personId: Long): Flow<List<PersonWithBalance>>

    @Query("SELECT * FROM persons WHERE id = :id")
    fun observeById(id: Long): Flow<Person?>

    @Query("SELECT * FROM persons WHERE id = :id")
    suspend fun getById(id: Long): Person?

    @Query("SELECT * FROM persons ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<Person>

    @Query("SELECT * FROM persons WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): Person?

    @Query("SELECT COUNT(*) FROM persons")
    suspend fun count(): Int

    @Insert
    suspend fun insert(person: Person): Long

    @Update
    suspend fun update(person: Person)

    @Delete
    suspend fun delete(person: Person)

    @Query("DELETE FROM persons")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(persons: List<Person>)
}

@Dao
interface TransactionDao {

    @Query(
        TX_SELECT + " WHERE t.personId = :personId ORDER BY t.createdAt DESC, t.id DESC"
    )
    fun observeForPerson(personId: Long): Flow<List<TxWithRemaining>>

    @Query(TX_SELECT + " WHERE t.id = :id LIMIT 1")
    fun observeById(id: Long): Flow<TxWithRemaining?>

    @Query(TX_SELECT + " WHERE t.id = :id LIMIT 1")
    suspend fun getById(id: Long): TxWithRemaining?

    @Query(
        TX_SELECT +
            " WHERE (:settled = -1 OR (:settled = 1 AND t.amount - COALESCE(pay.paid, 0) <= 0)" +
            " OR (:settled = 0 AND t.amount - COALESCE(pay.paid, 0) > 0))" +
            " AND (:personId = 0 OR t.personId = :personId)" +
            " AND (:categoryId = 0 OR t.categoryId = :categoryId)" +
            " AND (:from = 0 OR t.createdAt >= :from)" +
            " AND (:to = 0 OR t.createdAt <= :to)" +
            " ORDER BY t.createdAt DESC, t.id DESC"
    )
    suspend fun queryTransactions(
        from: Long,
        to: Long,
        personId: Long,
        categoryId: Long,
        settled: Int
    ): List<TxWithRemaining>

    @Query(
        TX_SELECT +
            " WHERE t.dueDate IS NOT NULL AND t.dueDate < :now" +
            " AND t.amount - COALESCE(pay.paid, 0) > 0 AND t.isArchived = 0" +
            " ORDER BY t.dueDate ASC"
    )
    suspend fun overdue(now: Long): List<TxWithRemaining>

    @Query(
        "SELECT " +
            "COALESCE(SUM(CASE WHEN t.direction = 1 AND t.amount - COALESCE(pay.paid, 0) > 0 " +
            "THEN t.amount - COALESCE(pay.paid, 0) ELSE 0 END), 0) AS totalCredit, " +
            "COALESCE(SUM(CASE WHEN t.direction = 0 AND t.amount - COALESCE(pay.paid, 0) > 0 " +
            "THEN t.amount - COALESCE(pay.paid, 0) ELSE 0 END), 0) AS totalDebt, " +
            "COUNT(DISTINCT CASE WHEN t.amount - COALESCE(pay.paid, 0) > 0 THEN t.personId END) AS openPersons, " +
            "COUNT(CASE WHEN t.dueDate IS NOT NULL AND t.dueDate >= :dayStart AND t.dueDate < :dayEnd " +
            "AND t.amount - COALESCE(pay.paid, 0) > 0 THEN 1 END) AS dueToday, " +
            "COUNT(CASE WHEN t.dueDate IS NOT NULL AND t.dueDate < :dayStart " +
            "AND t.amount - COALESCE(pay.paid, 0) > 0 THEN 1 END) AS overdue, " +
            "MIN(CASE WHEN t.dueDate IS NOT NULL AND t.dueDate >= :now " +
            "AND t.amount - COALESCE(pay.paid, 0) > 0 THEN t.dueDate END) AS nextDueDate " +
            "FROM transactions t " +
            "LEFT JOIN (SELECT transactionId, SUM(amount) AS paid FROM payments GROUP BY transactionId) pay " +
            "ON pay.transactionId = t.id " +
            "WHERE t.isArchived = 0"
    )
    fun observeStats(dayStart: Long, dayEnd: Long, now: Long): Flow<DashboardStats>

    @Insert
    suspend fun insert(tx: DebtTransaction): Long

    @Update
    suspend fun update(tx: DebtTransaction)

    @Delete
    suspend fun delete(tx: DebtTransaction)

    @Query("UPDATE transactions SET isArchived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(txs: List<DebtTransaction>)
}

@Dao
interface PaymentDao {

    @Query("SELECT * FROM payments WHERE transactionId = :txId ORDER BY date DESC, id DESC")
    fun observeForTx(txId: Long): Flow<List<Payment>>

    @Query(
        "SELECT pay.*, t.direction AS direction, p.name AS personName " +
            "FROM payments pay " +
            "JOIN transactions t ON t.id = pay.transactionId " +
            "JOIN persons p ON p.id = t.personId " +
            "WHERE t.personId = :personId " +
            "ORDER BY pay.date DESC, pay.id DESC"
    )
    fun observeForPerson(personId: Long): Flow<List<PaymentWithTx>>

    @Query(
        "SELECT pay.*, t.direction AS direction, p.name AS personName " +
            "FROM payments pay " +
            "JOIN transactions t ON t.id = pay.transactionId " +
            "JOIN persons p ON p.id = t.personId " +
            "WHERE pay.date >= :from AND pay.date <= :to AND (:personId = 0 OR t.personId = :personId) " +
            "ORDER BY pay.date DESC, pay.id DESC"
    )
    suspend fun inRange(from: Long, to: Long, personId: Long): List<PaymentWithTx>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE transactionId = :txId")
    suspend fun sumFor(txId: Long): Long

    @Query("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE transactionId = :txId AND id != :excludeId")
    suspend fun sumForExcept(txId: Long, excludeId: Long): Long

    @Insert
    suspend fun insert(payment: Payment): Long

    @Update
    suspend fun update(payment: Payment)

    @Delete
    suspend fun delete(payment: Payment)

    @Query("DELETE FROM payments")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(payments: List<Payment>)
}

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders WHERE transactionId = :txId LIMIT 1")
    suspend fun getForTx(txId: Long): Reminder?

    @Query("SELECT * FROM reminders WHERE isActive = 1 AND triggerAt > :now")
    suspend fun allActive(now: Long): List<Reminder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reminder: Reminder)

    @Query("UPDATE reminders SET isActive = 0 WHERE transactionId = :txId")
    suspend fun deactivate(txId: Long)

    @Query("DELETE FROM reminders WHERE transactionId = :txId")
    suspend fun deleteForTx(txId: Long)

    @Query("DELETE FROM reminders")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(reminders: List<Reminder>)
}

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY name")
    fun observeAll(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY name")
    suspend fun getAll(): List<Category>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Category?

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Insert
    suspend fun insert(category: Category): Long

    @Insert
    suspend fun insertAll(categories: List<Category>)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}
