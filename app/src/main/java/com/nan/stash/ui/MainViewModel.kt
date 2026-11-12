package com.nan.stash.ui

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nan.stash.data.local.AppDatabase
import com.nan.stash.data.local.SettingsDataStore
import com.nan.stash.data.local.StashSettings
import com.nan.stash.data.local.ThemeMode
import com.nan.stash.data.local.TransactionEntity
import com.nan.stash.data.model.PaymentMode
import com.nan.stash.data.model.TransactionType
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
    private val settingsDataStore = SettingsDataStore(application)

    val transactions: StateFlow<List<TransactionEntity>> = dao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val loans: StateFlow<List<com.nan.stash.data.local.LoanEntity>> = loanDao.getAllLoans()
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
            loanDao.insertLoan(
                com.nan.stash.data.local.LoanEntity(
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
    }

    fun recordRepayment(
        loan: com.nan.stash.data.local.LoanEntity,
        amountRepaid: Double,
        paymentMode: PaymentMode,
        note: String = ""
    ) {
        if (amountRepaid <= 0.0) return
        viewModelScope.launch {
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
    }

    fun addMoreToLoan(
        loan: com.nan.stash.data.local.LoanEntity,
        additionalAmount: Double,
        paymentMode: PaymentMode,
        note: String = ""
    ) {
        if (additionalAmount <= 0.0) return
        viewModelScope.launch {
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
    }

    fun deleteLoan(loan: com.nan.stash.data.local.LoanEntity) {
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
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        if (transaction.amount <= 0.0) return
        viewModelScope.launch {
            dao.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            dao.deleteTransaction(transaction)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            dao.clearAllTransactions()
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

    // Local Backup to JSON
    fun createBackupJson(transactions: List<TransactionEntity>): File {
        val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "stash_backup_$timestampStr.json"

        val root = JSONObject()
        root.put("version", 1)
        root.put("appName", "Stash")
        root.put("timestamp", System.currentTimeMillis())

        val array = JSONArray()
        for (t in transactions) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("type", t.type.name)
            obj.put("amount", t.amount)
            obj.put("tag", t.tag)
            obj.put("note", t.note)
            obj.put("timestamp", t.timestamp)
            obj.put("paymentMode", t.paymentMode.name)
            array.put(obj)
        }
        root.put("transactions", array)

        return saveFileToStorage(filename, root.toString(2))
    }

    // Restore from JSON
    fun restoreFromJson(jsonString: String, onResult: (Boolean, Int) -> Unit) {
        viewModelScope.launch {
            try {
                val root = JSONObject(jsonString)
                val array = root.getJSONArray("transactions")
                val list = mutableListOf<TransactionEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val type = try { TransactionType.valueOf(obj.getString("type")) } catch (_: Exception) { TransactionType.EXPENSE }
                    val paymentMode = try { PaymentMode.valueOf(obj.optString("paymentMode", "ONLINE")) } catch (_: Exception) { PaymentMode.ONLINE }
                    list.add(
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
                if (list.isNotEmpty()) {
                    dao.insertAll(list)
                }
                onResult(true, list.size)
            } catch (e: Exception) {
                onResult(false, 0)
            }
        }
    }

    private fun saveFileToStorage(filename: String, content: String): File {
        val context = getApplication<Application>()
        // First try standard external public Downloads
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val file = File(downloadsDir, filename)
            file.writeText(content)
            file
        } catch (_: Exception) {
            val fallbackDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            val file = File(fallbackDir, filename)
            file.writeText(content)
            file
        }
    }
}
