package com.nan.stash.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nan.stash.data.model.PaymentMode
import com.nan.stash.data.model.TransactionType

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN note TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN paymentMode TEXT NOT NULL DEFAULT 'ONLINE'")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `loans` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `personName` TEXT NOT NULL,
                `phoneNumber` TEXT NOT NULL,
                `totalAmount` REAL NOT NULL,
                `remainingAmount` REAL NOT NULL,
                `reason` TEXT NOT NULL,
                `lendDate` INTEGER NOT NULL,
                `dueDate` INTEGER,
                `initialPaymentMode` TEXT NOT NULL,
                `isSettled` INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `subscriptions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `amount` REAL NOT NULL,
                `category` TEXT NOT NULL,
                `paymentMode` TEXT NOT NULL DEFAULT 'ONLINE',
                `billingCycle` TEXT NOT NULL DEFAULT 'MONTHLY',
                `dueDay` INTEGER NOT NULL DEFAULT 1,
                `startDate` INTEGER NOT NULL,
                `isActive` INTEGER NOT NULL DEFAULT 1,
                `lastPaidDate` INTEGER
            )
        """.trimIndent())
    }
}

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = try {
        TransactionType.valueOf(value)
    } catch (_: Exception) {
        TransactionType.EXPENSE
    }

    @TypeConverter
    fun fromPaymentMode(value: PaymentMode): String = value.name

    @TypeConverter
    fun toPaymentMode(value: String): PaymentMode = try {
        PaymentMode.valueOf(value)
    } catch (_: Exception) {
        PaymentMode.ONLINE
    }
}

@Database(entities = [TransactionEntity::class, LoanEntity::class, SubscriptionEntity::class], version = 5, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun loanDao(): LoanDao
    abstract fun subscriptionDao(): SubscriptionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val ALL_MIGRATIONS = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5
        )

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "stash_database"
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
