package com.nan.stash.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nan.stash.data.model.PaymentMode

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val personName: String,
    val phoneNumber: String = "",
    val totalAmount: Double,
    val remainingAmount: Double,
    val reason: String = "",
    val lendDate: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val initialPaymentMode: PaymentMode = PaymentMode.CASH,
    val isSettled: Boolean = false
)
