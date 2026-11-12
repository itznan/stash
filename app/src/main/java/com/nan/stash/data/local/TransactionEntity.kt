package com.nan.stash.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nan.stash.data.model.PaymentMode
import com.nan.stash.data.model.TransactionType

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: TransactionType,
    val amount: Double,
    val tag: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val paymentMode: PaymentMode = PaymentMode.ONLINE
)
