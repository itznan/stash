package com.nan.stash.ui

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.nan.stash.data.local.AppDatabase
import com.nan.stash.data.local.LoanEntity
import com.nan.stash.data.local.SettingsDataStore
import com.nan.stash.data.local.StashSettings
import com.nan.stash.data.local.SubscriptionEntity
import com.nan.stash.data.local.ThemeMode
import com.nan.stash.data.local.TransactionEntity
import com.nan.stash.data.model.PaymentMode
import com.nan.stash.data.model.TransactionType
import com.nan.stash.ui.widget.StashWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CategorySummary(
    val tag: String,
    val amount: Double,
    val percentage: Float
)

data class DayTrend(
    val dayLabel: String,
    val fullDayName: String = "",
    val isToday: Boolean = false,
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val net: Double = 0.0,
    val amount: Double = 0.0,
    val isPositive: Boolean = true,
    val hasActivity: Boolean = false
)

data class MonthComparison(
    val currentMonthLabel: String = "",
    val previousMonthLabel: String = "",
    val currentIncome: Double = 0.0,
    val previousIncome: Double = 0.0,
    val currentExpense: Double = 0.0,
    val previousExpense: Double = 0.0,
    val currentNet: Double = 0.0,
    val previousNet: Double = 0.0,
    val expenseDiffPercentage: Float = 0f,
    val incomeDiffPercentage: Float = 0f,
    val hasComparisonData: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.transactionDao()
    private val loanDao = db.loanDao()
    private val subscriptionDao = db.subscriptionDao()
    private val settingsDataStore = SettingsDataStore(application)

    val transactions: StateFlow<List<TransactionEntity>> = dao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val loans: StateFlow<List<LoanEntity>> = loanDao.getAllLoans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subscriptions: StateFlow<List<SubscriptionEntity>> = subscriptionDao.getAllSubscriptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<StashSettings> = settingsDataStore.settingsFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            StashSettings(isBiometricEnabled = settingsDataStore.isBiometricEnabledSync())
        )

    fun isBiometricEnabledSync(): Boolean = settingsDataStore.isBiometricEnabledSync()

    val weeklyTrends: StateFlow<List<DayTrend>> = dao.getAllTransactions()
        .map { calculateWeeklyTrends(it) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categorySummaries: StateFlow<List<CategorySummary>> = dao.getAllTransactions()
        .map { calculateCategorySummaries(it) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthComparison: StateFlow<MonthComparison> = dao.getAllTransactions()
        .map { calculateMonthComparison(it) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthComparison())

    private fun notifyWidgetUpdate() {
        StashWidgetProvider.updateAllWidgets(getApplication())
    }

    fun lendMoney(
        personName: String,
        phoneNumber: String,
        amount: Double,
        reason: String,
        dueDate: Long?,
        paymentMode: PaymentMode,
        lendDate: Long = System.currentTimeMillis()
    ) {
        if (amount <= 0.0 || personName.isBlank()) return
        viewModelScope.launch {
            db.withTransaction {
                loanDao.insertLoan(
                    LoanEntity(
                        personName = personName.trim(),
                        phoneNumber = phoneNumber.trim(),
                        totalAmount = amount,
                        remainingAmount = amount,
                        reason = reason.trim(),
                        lendDate = lendDate,
                        dueDate = dueDate,
                        initialPaymentMode = paymentMode,
                        isSettled = false
                    )
                )
                dao.insertTransaction(
                    TransactionEntity(
                        type = TransactionType.LEND,
                        amount = amount,
                        tag = "Lent: ${personName.trim()}",
                        note = if (reason.isNotBlank()) "Reason: ${reason.trim()}" else "Lent money",
                        timestamp = lendDate,
                        paymentMode = paymentMode
                    )
                )
            }
            notifyWidgetUpdate()
        }
    }

    fun recordRepayment(
        loan: LoanEntity,
        amountRepaid: Double,
        paymentMode: PaymentMode,
        note: String = ""
    ) {
        if (amountRepaid <= 0.0) return
        viewModelScope.launch {
            db.withTransaction {
                val newRemaining = (loan.remainingAmount - amountRepaid).coerceAtLeast(0.0)
                val settled = newRemaining <= 0.009
                val updatedLoan = loan.copy(
                    remainingAmount = newRemaining,
                    isSettled = settled
                )
                loanDao.updateLoan(updatedLoan)

                val repaymentNote = if (note.isNotBlank()) note else "Repayment from ${loan.personName}"
                dao.insertTransaction(
                    TransactionEntity(
                        type = TransactionType.REPAYMENT,
                        amount = amountRepaid,
                        tag = "Repaid: ${loan.personName}",
                        note = repaymentNote,
                        timestamp = System.currentTimeMillis(),
                        paymentMode = paymentMode
                    )
                )
            }
            notifyWidgetUpdate()
        }
    }

    fun addMoreToLoan(
        loan: LoanEntity,
        additionalAmount: Double,
        paymentMode: PaymentMode,
        note: String = ""
    ) {
        if (additionalAmount <= 0.0) return
        viewModelScope.launch {
            db.withTransaction {
                val updatedLoan = loan.copy(
                    totalAmount = loan.totalAmount + additionalAmount,
                    remainingAmount = loan.remainingAmount + additionalAmount,
                    isSettled = false
                )
                loanDao.updateLoan(updatedLoan)

                val lendNote = if (note.isNotBlank()) note else "Additional loan to ${loan.personName}"
                dao.insertTransaction(
                    TransactionEntity(
                        type = TransactionType.LEND,
                        amount = additionalAmount,
                        tag = "Lent: ${loan.personName}",
                        note = lendNote,
                        timestamp = System.currentTimeMillis(),
                        paymentMode = paymentMode
                    )
                )
            }
            notifyWidgetUpdate()
        }
    }

    fun deleteLoan(loan: LoanEntity) {
        viewModelScope.launch {
            loanDao.deleteLoan(loan)
        }
    }

    fun addTransaction(
        type: TransactionType,
        amount: Double,
        tag: String,
        note: String,
        timestamp: Long,
        paymentMode: PaymentMode = PaymentMode.ONLINE
    ) {
        if (amount <= 0.0) return
        viewModelScope.launch {
            dao.insertTransaction(
                TransactionEntity(
                    type = type,
                    amount = amount,
                    tag = tag,
                    note = note,
                    timestamp = timestamp,
                    paymentMode = paymentMode
                )
            )
            notifyWidgetUpdate()
        }
    }

    fun addTransfer(
        amount: Double,
        fromMode: PaymentMode,
        note: String = "",
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (amount <= 0.0) return
        val directionTag = if (fromMode == PaymentMode.ONLINE) "Online → Cash" else "Cash → Online"
        viewModelScope.launch {
            dao.insertTransaction(
                TransactionEntity(
                    type = TransactionType.TRANSFER,
                    amount = amount,
                    tag = directionTag,
                    note = note,
                    timestamp = timestamp,
                    paymentMode = fromMode
                )
            )
            notifyWidgetUpdate()
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        if (transaction.amount <= 0.0) return
        viewModelScope.launch {
            dao.updateTransaction(transaction)
            notifyWidgetUpdate()
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            dao.deleteTransaction(transaction)
            notifyWidgetUpdate()
        }
    }

    // Subscriptions Management
    fun addSubscription(
        name: String,
        amount: Double,
        category: String,
        paymentMode: PaymentMode,
        dueDay: Int,
        billingCycle: String = "MONTHLY"
    ) {
        if (name.isBlank() || amount <= 0.0) return
        viewModelScope.launch {
            subscriptionDao.insertSubscription(
                SubscriptionEntity(
                    name = name.trim(),
                    amount = amount,
                    category = category.trim(),
                    paymentMode = paymentMode,
                    dueDay = dueDay.coerceIn(1, 31),
                    billingCycle = billingCycle
                )
            )
        }
    }

    fun updateSubscription(subscription: SubscriptionEntity) {
        viewModelScope.launch {
            subscriptionDao.updateSubscription(subscription)
        }
    }

    fun deleteSubscription(subscription: SubscriptionEntity) {
        viewModelScope.launch {
            subscriptionDao.deleteSubscription(subscription)
        }
    }

    fun logSubscriptionPayment(
        subscription: SubscriptionEntity,
        paymentMode: PaymentMode = subscription.paymentMode
    ) {
        viewModelScope.launch {
            db.withTransaction {
                val now = System.currentTimeMillis()
                subscriptionDao.updateSubscription(subscription.copy(lastPaidDate = now))
                dao.insertTransaction(
                    TransactionEntity(
                        type = TransactionType.EXPENSE,
                        amount = subscription.amount,
                        tag = subscription.category,
                        note = "Subscription: ${subscription.name}",
                        timestamp = now,
                        paymentMode = paymentMode
                    )
                )
            }
            notifyWidgetUpdate()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            db.withTransaction {
                dao.clearAllTransactions()
                loanDao.clearAllLoans()
                subscriptionDao.clearAllSubscriptions()
            }
            notifyWidgetUpdate()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsDataStore.setThemeMode(mode)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setDynamicColor(enabled)
        }
    }

    fun setCurrencySymbol(symbol: String) {
        viewModelScope.launch {
            settingsDataStore.setCurrencySymbol(symbol)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setBiometricEnabled(enabled)
        }
    }

    // Helper functions for UI
    fun calculateCategorySummaries(transactions: List<TransactionEntity>): List<CategorySummary> {
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        val totalExpense = expenses.sumOf { it.amount }
        if (totalExpense <= 0.0) return emptyList()

        return expenses.groupBy { it.tag }
            .map { (tag, list) ->
                val sum = list.sumOf { it.amount }
                CategorySummary(
                    tag = tag,
                    amount = sum,
                    percentage = (sum / totalExpense * 100).toFloat()
                )
            }
            .sortedByDescending { it.amount }
    }

    fun calculateWeeklyTrends(transactions: List<TransactionEntity>): List<DayTrend> {
        val days = listOf("M", "T", "W", "T", "F", "S", "S")
        val fullDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val nowCal = Calendar.getInstance()
        val currentDayIndex = (nowCal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday = 0 .. Sunday = 6

        // Compute start of Monday of the current week
        val mondayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -currentDayIndex)
        }

        return (0..6).map { i ->
            val dayCal = (mondayCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, i)
            }
            val startOfDay = dayCal.timeInMillis
            val endOfDay = startOfDay + 24 * 60 * 60 * 1000L

            val dayTransactions = transactions.filter { it.timestamp in startOfDay until endOfDay }
            val income = dayTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expense = dayTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val net = income - expense
            val hasActivity = dayTransactions.isNotEmpty()

            DayTrend(
                dayLabel = days[i],
                fullDayName = fullDays[i],
                isToday = (i == currentDayIndex),
                income = income,
                expense = expense,
                net = net,
                amount = Math.abs(net),
                isPositive = net >= 0,
                hasActivity = hasActivity
            )
        }
    }

    // Month-over-Month Comparison
    fun calculateMonthComparison(transactions: List<TransactionEntity>): MonthComparison {
        val now = Calendar.getInstance()
        val curYear = now.get(Calendar.YEAR)
        val curMonth = now.get(Calendar.MONTH) // 0-based

        val curMonthStart = Calendar.getInstance().apply {
            set(curYear, curMonth, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val nextMonthStart = Calendar.getInstance().apply {
            set(curYear, curMonth, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, 1)
        }.timeInMillis

        val prevMonthCal = Calendar.getInstance().apply {
            set(curYear, curMonth, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, -1)
        }
        val prevMonthStart = prevMonthCal.timeInMillis

        val curMonthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(curMonthStart))
        val prevMonthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(prevMonthStart))

        val curTx = transactions.filter { it.timestamp in curMonthStart until nextMonthStart }
        val prevTx = transactions.filter { it.timestamp in prevMonthStart until curMonthStart }

        val curIncome = curTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val curExpense = curTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val curNet = curIncome - curExpense

        val prevIncome = prevTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val prevExpense = prevTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val prevNet = prevIncome - prevExpense

        val expenseDiffPct = if (prevExpense > 0.0) {
            (((curExpense - prevExpense) / prevExpense) * 100).toFloat()
        } else if (curExpense > 0.0) {
            100f
        } else 0f

        val incomeDiffPct = if (prevIncome > 0.0) {
            (((curIncome - prevIncome) / prevIncome) * 100).toFloat()
        } else if (curIncome > 0.0) {
            100f
        } else 0f

        return MonthComparison(
            currentMonthLabel = curMonthName,
            previousMonthLabel = prevMonthName,
            currentIncome = curIncome,
            previousIncome = prevIncome,
            currentExpense = curExpense,
            previousExpense = prevExpense,
            currentNet = curNet,
            previousNet = prevNet,
            expenseDiffPercentage = expenseDiffPct,
            incomeDiffPercentage = incomeDiffPct,
            hasComparisonData = curTx.isNotEmpty() || prevTx.isNotEmpty()
        )
    }

    // Export to CSV
    fun exportToCsv(transactions: List<TransactionEntity>): File {
        val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "stash_transactions_$timestampStr.csv"

        val sb = StringBuilder()
        sb.append("ID,Type,Amount,Category,PaymentMode,Note,Date,Time\n")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

        for (t in transactions) {
            val d = Date(t.timestamp)
            val cleanNote = t.note.replace(",", " ").replace("\n", " ")
            sb.append("${t.id},${t.type.name},${String.format(Locale.US, "%.2f", t.amount)},\"${t.tag}\",${t.paymentMode.name},\"$cleanNote\",${dateFormat.format(d)},${timeFormat.format(d)}\n")
        }

        return saveFileToStorage(filename, sb.toString())
    }

    // Export to PDF Statement
    fun exportToPdf(transactions: List<TransactionEntity>, currencySymbol: String): File {
        return com.nan.stash.ui.util.PdfStatementGenerator.generateStatement(
            getApplication(),
            transactions,
            currencySymbol
        )
    }

    // Local Backup to JSON (Includes transactions, loans, and subscriptions)
    fun createBackupJson(
        transactions: List<TransactionEntity> = this.transactions.value,
        loans: List<LoanEntity> = this.loans.value,
        subscriptions: List<SubscriptionEntity> = this.subscriptions.value
    ): File {
        val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "stash_backup_$timestampStr.json"

        val root = JSONObject()
        root.put("version", 3)
        root.put("appName", "Stash")
        root.put("timestamp", System.currentTimeMillis())

        val txArray = JSONArray()
        for (t in transactions) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("type", t.type.name)
            obj.put("amount", t.amount)
            obj.put("tag", t.tag)
            obj.put("note", t.note)
            obj.put("timestamp", t.timestamp)
            obj.put("paymentMode", t.paymentMode.name)
            txArray.put(obj)
        }
        root.put("transactions", txArray)

        val loanArray = JSONArray()
        for (l in loans) {
            val obj = JSONObject()
            obj.put("id", l.id)
            obj.put("personName", l.personName)
            obj.put("phoneNumber", l.phoneNumber)
            obj.put("totalAmount", l.totalAmount)
            obj.put("remainingAmount", l.remainingAmount)
            obj.put("reason", l.reason)
            obj.put("lendDate", l.lendDate)
            if (l.dueDate != null) {
                obj.put("dueDate", l.dueDate)
            }
            obj.put("initialPaymentMode", l.initialPaymentMode.name)
            obj.put("isSettled", l.isSettled)
            loanArray.put(obj)
        }
        root.put("loans", loanArray)

        val subArray = JSONArray()
        for (s in subscriptions) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("name", s.name)
            obj.put("amount", s.amount)
            obj.put("category", s.category)
            obj.put("paymentMode", s.paymentMode.name)
            obj.put("billingCycle", s.billingCycle)
            obj.put("dueDay", s.dueDay)
            obj.put("startDate", s.startDate)
            obj.put("isActive", s.isActive)
            if (s.lastPaidDate != null) {
                obj.put("lastPaidDate", s.lastPaidDate)
            }
            subArray.put(obj)
        }
        root.put("subscriptions", subArray)

        return saveFileToStorage(filename, root.toString(2))
    }

    // Restore from JSON (Restores transactions, loans, and subscriptions atomically)
    fun restoreFromJson(jsonString: String, onResult: (Boolean, Int) -> Unit) {
        viewModelScope.launch {
            try {
                val root = JSONObject(jsonString)
                val txList = mutableListOf<TransactionEntity>()
                if (root.has("transactions")) {
                    val txArray = root.getJSONArray("transactions")
                    for (i in 0 until txArray.length()) {
                        val obj = txArray.getJSONObject(i)
                        val type = try { TransactionType.valueOf(obj.getString("type")) } catch (_: Exception) { TransactionType.EXPENSE }
                        val paymentMode = try { PaymentMode.valueOf(obj.optString("paymentMode", "ONLINE")) } catch (_: Exception) { PaymentMode.ONLINE }
                        txList.add(
                            TransactionEntity(
                                id = 0,
                                type = type,
                                amount = obj.getDouble("amount"),
                                tag = obj.getString("tag"),
                                note = obj.optString("note", ""),
                                timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                                paymentMode = paymentMode
                            )
                        )
                    }
                }

                val loanList = mutableListOf<LoanEntity>()
                if (root.has("loans")) {
                    val loanArray = root.getJSONArray("loans")
                    for (i in 0 until loanArray.length()) {
                        val obj = loanArray.getJSONObject(i)
                        val paymentMode = try {
                            PaymentMode.valueOf(obj.optString("initialPaymentMode", "CASH"))
                        } catch (_: Exception) {
                            PaymentMode.CASH
                        }
                        val dueDate = if (obj.has("dueDate") && !obj.isNull("dueDate")) obj.getLong("dueDate") else null
                        loanList.add(
                            LoanEntity(
                                id = 0,
                                personName = obj.getString("personName"),
                                phoneNumber = obj.optString("phoneNumber", ""),
                                totalAmount = obj.getDouble("totalAmount"),
                                remainingAmount = obj.getDouble("remainingAmount"),
                                reason = obj.optString("reason", ""),
                                lendDate = obj.optLong("lendDate", System.currentTimeMillis()),
                                dueDate = dueDate,
                                initialPaymentMode = paymentMode,
                                isSettled = obj.optBoolean("isSettled", false)
                            )
                        )
                    }
                }

                val subList = mutableListOf<SubscriptionEntity>()
                if (root.has("subscriptions")) {
                    val subArray = root.getJSONArray("subscriptions")
                    for (i in 0 until subArray.length()) {
                        val obj = subArray.getJSONObject(i)
                        val paymentMode = try {
                            PaymentMode.valueOf(obj.optString("paymentMode", "ONLINE"))
                        } catch (_: Exception) {
                            PaymentMode.ONLINE
                        }
                        val lastPaid = if (obj.has("lastPaidDate") && !obj.isNull("lastPaidDate")) obj.getLong("lastPaidDate") else null
                        subList.add(
                            SubscriptionEntity(
                                id = 0,
                                name = obj.getString("name"),
                                amount = obj.getDouble("amount"),
                                category = obj.optString("category", "Bills"),
                                paymentMode = paymentMode,
                                billingCycle = obj.optString("billingCycle", "MONTHLY"),
                                dueDay = obj.optInt("dueDay", 1),
                                startDate = obj.optLong("startDate", System.currentTimeMillis()),
                                isActive = obj.optBoolean("isActive", true),
                                lastPaidDate = lastPaid
                            )
                        )
                    }
                }

                db.withTransaction {
                    if (txList.isNotEmpty()) {
                        dao.insertAll(txList)
                    }
                    if (loanList.isNotEmpty()) {
                        loanDao.insertAll(loanList)
                    }
                    if (subList.isNotEmpty()) {
                        subscriptionDao.insertAll(subList)
                    }
                }
                notifyWidgetUpdate()
                val totalRestored = txList.size + loanList.size + subList.size
                onResult(true, totalRestored)
            } catch (e: Exception) {
                onResult(false, 0)
            }
        }
    }

    private fun saveFileToStorage(filename: String, content: String): File {
        val context = getApplication<Application>()
        val targetDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        if (!targetDir.exists()) targetDir.mkdirs()
        val file = File(targetDir, filename)
        file.writeText(content)
        return file
    }
}
