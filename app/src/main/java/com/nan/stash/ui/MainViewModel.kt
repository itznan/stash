package com.nan.stash.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.nan.stash.data.backup.BackupManager
import com.nan.stash.data.local.AppDatabase
import com.nan.stash.data.local.LoanEntity
import com.nan.stash.data.local.SettingsDataStore
import com.nan.stash.data.local.StashSettings
import com.nan.stash.data.local.SubscriptionEntity
import com.nan.stash.data.local.ThemeMode
import com.nan.stash.data.local.TransactionEntity
import com.nan.stash.data.model.CategorySummary
import com.nan.stash.data.model.DayTrend
import com.nan.stash.data.model.MonthComparison
import com.nan.stash.data.model.PaymentMode
import com.nan.stash.data.model.TransactionType
import com.nan.stash.ui.util.AnalyticsCalculator
import com.nan.stash.ui.util.DateUtils
import com.nan.stash.ui.util.PdfStatementGenerator
import com.nan.stash.ui.util.WalletBalances
import com.nan.stash.ui.widget.StashWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.transactionDao()
    private val loanDao = db.loanDao()
    private val subscriptionDao = db.subscriptionDao()
    private val settingsDataStore = SettingsDataStore(application)
    private val currentDayKey = MutableStateFlow(DateUtils.formatDateKey(System.currentTimeMillis()))

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

    fun refreshDateIfNeeded() {
        val today = DateUtils.formatDateKey(System.currentTimeMillis())
        if (currentDayKey.value != today) {
            currentDayKey.value = today
        }
    }

    val balances: StateFlow<WalletBalances> = transactions
        .map { AnalyticsCalculator.calculateWalletBalances(it) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WalletBalances())

    val weeklyTrends: StateFlow<List<DayTrend>> = combine(transactions, currentDayKey) { txs, _ ->
        AnalyticsCalculator.calculateWeeklyTrends(txs)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categorySummaries: StateFlow<List<CategorySummary>> = combine(transactions, currentDayKey) { txs, _ ->
        AnalyticsCalculator.calculateCategorySummaries(txs)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthComparison: StateFlow<MonthComparison> = combine(transactions, currentDayKey) { txs, _ ->
        AnalyticsCalculator.calculateMonthComparison(txs)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthComparison())

    private fun notifyWidgetUpdate() {
        StashWidgetProvider.updateAllWidgets(getApplication())
    }

    // Loans Management
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
                val currentLoan = loanDao.getLoanById(loan.id) ?: return@withTransaction
                loanDao.updateLoan(AnalyticsCalculator.applyRepayment(currentLoan, amountRepaid))

                val repaymentNote = if (note.isNotBlank()) note else "Repayment from ${currentLoan.personName}"
                dao.insertTransaction(
                    TransactionEntity(
                        type = TransactionType.REPAYMENT,
                        amount = amountRepaid,
                        tag = "Repaid: ${currentLoan.personName}",
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
                val currentLoan = loanDao.getLoanById(loan.id) ?: return@withTransaction
                loanDao.updateLoan(AnalyticsCalculator.applyAdditionalLoan(currentLoan, additionalAmount))

                val lendNote = if (note.isNotBlank()) note else "Additional loan to ${currentLoan.personName}"
                dao.insertTransaction(
                    TransactionEntity(
                        type = TransactionType.LEND,
                        amount = additionalAmount,
                        tag = "Lent: ${currentLoan.personName}",
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

    // Transactions Management
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

    fun clearTransactions() {
        viewModelScope.launch {
            dao.clearAllTransactions()
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
            notifyWidgetUpdate()
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setBiometricEnabled(enabled)
        }
    }

    fun setLowBalanceWarningEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setLowBalanceWarningEnabled(enabled)
        }
    }

    fun setLowOnlineThreshold(amount: Double) {
        viewModelScope.launch {
            settingsDataStore.setLowOnlineThreshold(amount)
        }
    }

    fun setLowCashThreshold(amount: Double) {
        viewModelScope.launch {
            settingsDataStore.setLowCashThreshold(amount)
        }
    }

    // Export & Backup
    suspend fun exportToCsv(transactions: List<TransactionEntity>): File =
        withContext(Dispatchers.IO) {
            BackupManager.exportToCsv(getApplication(), transactions)
        }

    suspend fun exportToPdf(transactions: List<TransactionEntity>, currencySymbol: String): File =
        withContext(Dispatchers.IO) {
            PdfStatementGenerator.generateStatement(getApplication(), transactions, currencySymbol)
        }

    suspend fun createBackupJson(
        transactions: List<TransactionEntity> = this.transactions.value,
        loans: List<LoanEntity> = this.loans.value,
        subscriptions: List<SubscriptionEntity> = this.subscriptions.value
    ): File = withContext(Dispatchers.IO) {
        BackupManager.createBackupJson(getApplication(), transactions, loans, subscriptions)
    }

    fun restoreFromJson(jsonString: String, onResult: (Boolean, Int) -> Unit) {
        viewModelScope.launch {
            try {
                val restored = withContext(Dispatchers.IO) {
                    BackupManager.parseBackupJson(jsonString)
                }
                val totalRestored = restored.transactions.size + restored.loans.size + restored.subscriptions.size
                if (totalRestored == 0) {
                    onResult(false, 0)
                    return@launch
                }
                db.withTransaction {
                    dao.clearAllTransactions()
                    loanDao.clearAllLoans()
                    subscriptionDao.clearAllSubscriptions()
                    if (restored.transactions.isNotEmpty()) dao.insertAll(restored.transactions)
                    if (restored.loans.isNotEmpty()) loanDao.insertAll(restored.loans)
                    if (restored.subscriptions.isNotEmpty()) subscriptionDao.insertAll(restored.subscriptions)
                }
                notifyWidgetUpdate()
                onResult(true, totalRestored)
            } catch (e: Exception) {
                onResult(false, 0)
            }
        }
    }
}
