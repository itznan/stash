package com.nan.stash

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nan.stash.data.local.AppDatabase
import com.nan.stash.data.local.MIGRATION_1_2
import com.nan.stash.data.local.MIGRATION_2_3
import com.nan.stash.data.local.MIGRATION_3_4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate1To2() {
        var db = helper.createDatabase(TEST_DB, 1).apply {
            execSQL("INSERT INTO transactions (id, type, amount, tag, timestamp) VALUES (1, 'EXPENSE', 42.50, 'food', 1700000000000)")
            close()
        }

        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        val cursor = db.query("SELECT id, type, amount, tag, note, timestamp FROM transactions WHERE id = 1")
        assertTrue(cursor.moveToFirst())
        assertEquals(1L, cursor.getLong(cursor.getColumnIndexOrThrow("id")))
        assertEquals("EXPENSE", cursor.getString(cursor.getColumnIndexOrThrow("type")))
        assertEquals(42.50, cursor.getDouble(cursor.getColumnIndexOrThrow("amount")), 0.001)
        assertEquals("food", cursor.getString(cursor.getColumnIndexOrThrow("tag")))
        assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("note")))
        assertEquals(1700000000000L, cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")))
        cursor.close()
    }

    @Test
    fun migrate2To3() {
        var db = helper.createDatabase(TEST_DB, 2).apply {
            execSQL("INSERT INTO transactions (id, type, amount, tag, note, timestamp) VALUES (2, 'INCOME', 1500.00, 'salary', 'Monthly paycheck', 1700000001000)")
            close()
        }

        db = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)

        val cursor = db.query("SELECT id, type, amount, tag, note, timestamp, paymentMode FROM transactions WHERE id = 2")
        assertTrue(cursor.moveToFirst())
        assertEquals(2L, cursor.getLong(cursor.getColumnIndexOrThrow("id")))
        assertEquals("INCOME", cursor.getString(cursor.getColumnIndexOrThrow("type")))
        assertEquals(1500.00, cursor.getDouble(cursor.getColumnIndexOrThrow("amount")), 0.001)
        assertEquals("salary", cursor.getString(cursor.getColumnIndexOrThrow("tag")))
        assertEquals("Monthly paycheck", cursor.getString(cursor.getColumnIndexOrThrow("note")))
        assertEquals(1700000001000L, cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")))
        assertEquals("ONLINE", cursor.getString(cursor.getColumnIndexOrThrow("paymentMode")))
        cursor.close()
    }

    @Test
    fun migrate3To4() {
        var db = helper.createDatabase(TEST_DB, 3).apply {
            execSQL("INSERT INTO transactions (id, type, amount, tag, note, timestamp, paymentMode) VALUES (3, 'EXPENSE', 50.0, 'grocery', '', 1700000002000, 'ONLINE')")
            close()
        }

        db = helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4)

        // Verify transactions table preserved
        val txCursor = db.query("SELECT id, type, amount FROM transactions WHERE id = 3")
        assertTrue(txCursor.moveToFirst())
        assertEquals(3L, txCursor.getLong(txCursor.getColumnIndexOrThrow("id")))
        txCursor.close()

        // Verify loans table created
        db.execSQL("INSERT INTO loans (id, personName, phoneNumber, totalAmount, remainingAmount, reason, lendDate, dueDate, initialPaymentMode, isSettled) VALUES (1, 'Alice', '+1234567890', 100.0, 100.0, 'Emergency', 1700000000000, 1700086400000, 'CASH', 0)")
        val loanCursor = db.query("SELECT personName, remainingAmount, isSettled FROM loans WHERE id = 1")
        assertTrue(loanCursor.moveToFirst())
        assertEquals("Alice", loanCursor.getString(loanCursor.getColumnIndexOrThrow("personName")))
        assertEquals(100.0, loanCursor.getDouble(loanCursor.getColumnIndexOrThrow("remainingAmount")), 0.001)
        assertEquals(0, loanCursor.getInt(loanCursor.getColumnIndexOrThrow("isSettled")))
        loanCursor.close()
    }

    @Test
    fun migrateAll1To4() {
        var db = helper.createDatabase(TEST_DB, 1).apply {
            execSQL("INSERT INTO transactions (id, type, amount, tag, timestamp) VALUES (4, 'EXPENSE', 12.00, 'coffee', 1700000002000)")
            close()
        }

        db = helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)

        val cursor = db.query("SELECT id, type, amount, tag, note, timestamp, paymentMode FROM transactions WHERE id = 4")
        assertTrue(cursor.moveToFirst())
        assertEquals(4L, cursor.getLong(cursor.getColumnIndexOrThrow("id")))
        assertEquals("EXPENSE", cursor.getString(cursor.getColumnIndexOrThrow("type")))
        assertEquals(12.00, cursor.getDouble(cursor.getColumnIndexOrThrow("amount")), 0.001)
        assertEquals("coffee", cursor.getString(cursor.getColumnIndexOrThrow("tag")))
        assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("note")))
        assertEquals("ONLINE", cursor.getString(cursor.getColumnIndexOrThrow("paymentMode")))
        cursor.close()
    }
}
