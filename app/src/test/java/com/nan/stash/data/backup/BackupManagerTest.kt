package com.nan.stash.data.backup

import com.nan.stash.data.model.PaymentMode
import com.nan.stash.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
}
