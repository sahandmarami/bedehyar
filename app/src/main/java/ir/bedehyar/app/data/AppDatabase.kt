package ir.bedehyar.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database (version 2).
 *
 * Version 1 (legacy release 1.0) had a single flat `transactions` table:
 *   transactions(id, personName, amount, paidAmount, iOwe, note, dueAt, alarmEnabled, createdAt)
 *
 * Version 2 normalises it into persons / transactions / payments / reminders / categories.
 * MIGRATION_1_2 preserves every legacy row: person names become Person rows, the old
 * paidAmount becomes a real Payment row and pending alarms become Reminder rows.
 */
@Database(
    entities = [
        Person::class,
        DebtTransaction::class,
        Payment::class,
        Reminder::class,
        Category::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun personDao(): PersonDao
    abstract fun txDao(): TransactionDao
    abstract fun paymentDao(): PaymentDao
    abstract fun reminderDao(): ReminderDao
    abstract fun categoryDao(): CategoryDao

    companion object {

        const val DB_NAME = "bedehyar.db"

        val DEFAULT_CATEGORIES = listOf(
            "قرض", "خرید و فروش", "اجاره", "خدمات", "خانواده", "دوستان", "سایر"
        )

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(CREATE_CALLBACK)
                    .build()
                    .also { INSTANCE = it }
            }

        private val CREATE_CALLBACK = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                DEFAULT_CATEGORIES.forEach { name ->
                    db.execSQL(
                        "INSERT INTO categories (name) VALUES (?)", arrayOf(name)
                    )
                }
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val now = System.currentTimeMillis()

                // 1) keep the legacy table aside
                db.execSQL("ALTER TABLE transactions RENAME TO transactions_old")

                // 2) categories
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_name` ON `categories` (`name`)")
                DEFAULT_CATEGORIES.forEach { name ->
                    db.execSQL("INSERT INTO categories (name) VALUES (?)", arrayOf(name))
                }

                // 3) persons (one row per distinct legacy name)
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `persons` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `phone` TEXT NOT NULL, `note` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "INSERT INTO persons (name, phone, note, createdAt) " +
                        "SELECT personName, '', '', MIN(createdAt) FROM transactions_old GROUP BY personName"
                )

                // 4) transactions (new shape; legacy ids are preserved)
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `transactions` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`personId` INTEGER NOT NULL, " +
                        "`direction` INTEGER NOT NULL, " +
                        "`amount` INTEGER NOT NULL, " +
                        "`description` TEXT NOT NULL, " +
                        "`categoryId` INTEGER, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`dueDate` INTEGER, " +
                        "`reminderHour` INTEGER NOT NULL, " +
                        "`reminderMinute` INTEGER NOT NULL, " +
                        "`reminderEnabled` INTEGER NOT NULL, " +
                        "`isArchived` INTEGER NOT NULL, " +
                        "`privateNote` TEXT NOT NULL, " +
                        "FOREIGN KEY(personId) REFERENCES persons(id) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_personId` ON `transactions` (`personId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_dueDate` ON `transactions` (`dueDate`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_isArchived` ON `transactions` (`isArchived`)")
                db.execSQL(
                    "INSERT INTO transactions (id, personId, direction, amount, description, categoryId, createdAt, dueDate, reminderHour, reminderMinute, reminderEnabled, isArchived, privateNote) " +
                        "SELECT o.id, p.id, " +
                        "CASE WHEN o.iOwe = 1 THEN $DIRECTION_I_OWE ELSE $DIRECTION_OWES_ME END, " +
                        "o.amount, o.note, NULL, o.createdAt, o.dueAt, 9, 0, o.alarmEnabled, " +
                        "CASE WHEN o.paidAmount >= o.amount THEN 1 ELSE 0 END, '' " +
                        "FROM transactions_old o JOIN persons p ON p.name = o.personName"
                )

                // 5) payments — every legacy paidAmount becomes one Payment row
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `payments` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`transactionId` INTEGER NOT NULL, " +
                        "`amount` INTEGER NOT NULL, " +
                        "`date` INTEGER NOT NULL, " +
                        "`note` TEXT NOT NULL, " +
                        "FOREIGN KEY(transactionId) REFERENCES transactions(id) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_transactionId` ON `payments` (`transactionId`)")
                db.execSQL(
                    "INSERT INTO payments (transactionId, amount, date, note) " +
                        "SELECT o.id, o.paidAmount, o.createdAt, 'انتقال از نسخه پیشین' " +
                        "FROM transactions_old o WHERE o.paidAmount > 0"
                )

                // 6) reminders — only still-valid future alarms are carried over
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `reminders` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`transactionId` INTEGER NOT NULL, " +
                        "`triggerAt` INTEGER NOT NULL, " +
                        "`isActive` INTEGER NOT NULL, " +
                        "FOREIGN KEY(transactionId) REFERENCES transactions(id) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_reminders_transactionId` ON `reminders` (`transactionId`)")
                db.execSQL(
                    "INSERT INTO reminders (transactionId, triggerAt, isActive) " +
                        "SELECT o.id, o.dueAt, 1 FROM transactions_old o " +
                        "WHERE o.alarmEnabled = 1 AND o.dueAt IS NOT NULL AND o.dueAt > $now AND o.paidAmount < o.amount"
                )

                // 7) drop the legacy table
                db.execSQL("DROP TABLE transactions_old")
            }
        }
    }
}
