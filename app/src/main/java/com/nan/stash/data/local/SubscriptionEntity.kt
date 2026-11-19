package com.nan.stash.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nan.stash.data.model.PaymentMode

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val amount: Double,
    val category: String = "Bills",
    val paymentMode: PaymentMode = PaymentMode.ONLINE,
    val billingCycle: String = "MONTHLY",
    val dueDay: Int = 1,
    val startDate: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val lastPaidDate: Long? = null
)
