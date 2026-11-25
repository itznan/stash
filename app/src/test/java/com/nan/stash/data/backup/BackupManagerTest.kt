package com.nan.stash.data.backup

import com.nan.stash.data.local.LoanEntity
import com.nan.stash.data.local.SubscriptionEntity
import com.nan.stash.data.local.TransactionEntity
import com.nan.stash.data.model.PaymentMode
import com.nan.stash.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupManagerTest {

    @Test
    fun parseBackupJson_validPayload_parsesAllEntities() {
        val json = """
        {
          "version": 3,
          "appName": "Stash",
          "timestamp": 1700000000000,
          "transactions": [
            {
              "type": "EXPENSE",
              "amount": 25.50,
              "tag": "Food",
              "note": "Lunch, with drinks",
              "timestamp": 1700000001000,
              "paymentMode": "CASH"
            }
          ],
          "loans": [
            {
              "personName": "Alice",
              "phoneNumber": "1234567890",
              "totalAmount": 100.0,
              "remainingAmount": 40.0,
              "reason": "Trip",
              "lendDate": 1700000002000,
              "dueDate": 1705000000000,
              "initialPaymentMode": "ONLINE",
              "isSettled": false
            }
          ],
          "subscriptions": [
            {
              "name": "Cloud Storage",
              "amount": 4.99,
              "category": "Bills",
              "paymentMode": "ONLINE",
              "billingCycle": "MONTHLY",
              "dueDay": 15,
              "startDate": 1700000003000,
              "isActive": true,
              "lastPaidDate": 1700000004000
            }
          ]
        }
        """.trimIndent()

        val restored = BackupManager.parseBackupJson(json)

        // Transactions
        assertEquals(1, restored.transactions.size)
        val tx = restored.transactions[0]
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals(25.50, tx.amount, 0.001)
        assertEquals("Food", tx.tag)
        assertEquals("Lunch, with drinks", tx.note)
        assertEquals(PaymentMode.CASH, tx.paymentMode)

        // Loans
        assertEquals(1, restored.loans.size)
        val loan = restored.loans[0]
        assertEquals("Alice", loan.personName)
        assertEquals(100.0, loan.totalAmount, 0.001)
        assertEquals(40.0, loan.remainingAmount, 0.001)
        assertEquals(1705000000000L, loan.dueDate)

        // Subscriptions
        assertEquals(1, restored.subscriptions.size)
        val sub = restored.subscriptions[0]
        assertEquals("Cloud Storage", sub.name)
        assertEquals(4.99, sub.amount, 0.001)
        assertEquals(15, sub.dueDay)
    }

    @Test
    fun parseBackupJson_emptyOrPartialPayload_handlesMissingGracefully() {
        val json = """{"version": 1, "transactions": []}"""
        val restored = BackupManager.parseBackupJson(json)

        assertEquals(0, restored.transactions.size)
        assertEquals(0, restored.loans.size)
        assertEquals(0, restored.subscriptions.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseBackupJson_foreignJson_throwsException() {
        val json = """{"name": "some-random-package", "version": "1.0.0"}"""
        BackupManager.parseBackupJson(json)
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseBackupJson_emptyJson_throwsException() {
        val json = "{}"
        BackupManager.parseBackupJson(json)
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseBackupJson_unknownTransactionType_throwsException() {
        val json = """{"version": 3, "transactions": [{"type": "CRYPTO_SWAP", "amount": 10.0, "tag": "Swap"}]}"""
        BackupManager.parseBackupJson(json)
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseBackupJson_unknownPaymentMode_throwsException() {
        val json = """{"version": 3, "transactions": [{"type": "INCOME", "amount": 10.0, "tag": "Salary", "paymentMode": "BARTER"}]}"""
        BackupManager.parseBackupJson(json)
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseBackupJson_negativeAmount_throwsException() {
        val json = """{"version": 3, "transactions": [{"type": "EXPENSE", "amount": -15.0, "tag": "Food"}]}"""
        BackupManager.parseBackupJson(json)
    }

    @Test
    fun parseBackupJson_clampsSubscriptionDueDay() {
        val json = """
        {
          "version": 3,
          "subscriptions": [
            {"name": "A", "amount": 5.0, "dueDay": 99},
            {"name": "B", "amount": 5.0, "dueDay": 0}
          ]
        }
        """.trimIndent()
        val restored = BackupManager.parseBackupJson(json)
        assertEquals(31, restored.subscriptions[0].dueDay)
        assertEquals(1, restored.subscriptions[1].dueDay)
    }

    @Test
    fun buildBackupJsonContent_roundTripsAllEntities() {
        val txs = listOf(
            TransactionEntity(id = 1, type = TransactionType.INCOME, amount = 500.0, tag = "Salary", note = "May", timestamp = 1700000000000L, paymentMode = PaymentMode.ONLINE)
        )
        val loans = listOf(
            LoanEntity(id = 2, personName = "Bob", phoneNumber = "555-0100", totalAmount = 200.0, remainingAmount = 50.0, reason = "Trip", lendDate = 1700000001000L, dueDate = 1705000000000L, initialPaymentMode = PaymentMode.CASH, isSettled = false)
        )
        val subs = listOf(
            SubscriptionEntity(id = 3, name = "Music", amount = 9.99, category = "Bills", paymentMode = PaymentMode.ONLINE, billingCycle = "MONTHLY", dueDay = 10, startDate = 1700000002000L, isActive = true, lastPaidDate = 1700000003000L)
        )

        val json = BackupManager.buildBackupJsonContent(txs, loans, subs)
        val restored = BackupManager.parseBackupJson(json)

        assertEquals(1, restored.transactions.size)
        assertEquals(TransactionType.INCOME, restored.transactions[0].type)
        assertEquals(500.0, restored.transactions[0].amount, 0.001)
        assertEquals(1, restored.loans.size)
        assertEquals("Bob", restored.loans[0].personName)
        assertEquals(50.0, restored.loans[0].remainingAmount, 0.001)
        assertFalse(restored.loans[0].isSettled)
        assertEquals(1, restored.subscriptions.size)
        assertEquals("Music", restored.subscriptions[0].name)
        assertEquals(10, restored.subscriptions[0].dueDay)
    }

    @Test
    fun buildCsvContent_escapesQuotesAndNewlines() {
        val txs = listOf(
            TransactionEntity(
                id = 7,
                type = TransactionType.EXPENSE,
                amount = 12.5,
                tag = "Food, \"Out\"\n",
                note = "Line1\r\nLine2, \"extra\"",
                timestamp = 1700000000000L,
                paymentMode = PaymentMode.CASH
            )
        )
        val csv = BackupManager.buildCsvContent(txs)
        val lines = csv.trimEnd().lines()
        assertEquals(2, lines.size)
        assertTrue(lines[1].startsWith("7,EXPENSE,12.50,\"Food, \"\"Out\"\" \",CASH,\"Line1  Line2, \"\"extra\"\"\","))
    }
}
