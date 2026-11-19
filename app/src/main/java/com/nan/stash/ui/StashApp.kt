package com.nan.stash.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.nan.stash.data.local.ThemeMode
import com.nan.stash.data.local.TransactionEntity
import com.nan.stash.ui.components.FabQuickActionMenu
import com.nan.stash.ui.navigation.StashBottomNavigation
import com.nan.stash.ui.navigation.bottomNavScreens
import com.nan.stash.ui.screens.history.HistoryScreen
import com.nan.stash.ui.screens.home.HomeScreen
import com.nan.stash.ui.screens.loans.LendMoneyBottomSheet
import com.nan.stash.ui.screens.loans.LentMoneyManagerSheet
import com.nan.stash.ui.screens.settings.SettingsScreen
import com.nan.stash.ui.screens.subscriptions.SubscriptionManagerSheet
import com.nan.stash.ui.screens.tags.TagsScreen
import com.nan.stash.ui.screens.transactions.AddMoneyBottomSheet
import com.nan.stash.ui.screens.transactions.EditTransactionBottomSheet
import com.nan.stash.ui.screens.transactions.SpendMoneyBottomSheet
import com.nan.stash.ui.screens.transactions.TransferMoneyBottomSheet
import com.nan.stash.ui.theme.StashTheme
import com.nan.stash.ui.widget.StashWidgetProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun StashApp(
    viewModel: MainViewModel,
    pendingAction: String?,
    onClearPendingAction: () -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
    onBackupJson: () -> Unit,
    onRestoreJson: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val loans by viewModel.loans.collectAsState()
    val subscriptions by viewModel.subscriptions.collectAsState()
    val weeklyTrends by viewModel.weeklyTrends.collectAsState()
    val categorySummaries by viewModel.categorySummaries.collectAsState()
    val monthComparison by viewModel.monthComparison.collectAsState()

    val isDarkTheme = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    StashTheme(darkTheme = isDarkTheme) {
        val pagerState = rememberPagerState(initialPage = 0, pageCount = { bottomNavScreens.size })
        val coroutineScope = rememberCoroutineScope()
        var scrollJob by remember { mutableStateOf<Job?>(null) }
        val selectedIndex = if (pagerState.isScrollInProgress) {
            pagerState.targetPage.coerceIn(0, bottomNavScreens.lastIndex)
        } else {
            pagerState.currentPage.coerceIn(0, bottomNavScreens.lastIndex)
        }
        val currentRoute = bottomNavScreens[selectedIndex].route

        var showFabMenu by remember { mutableStateOf(false) }
        var showAddSheet by remember { mutableStateOf(false) }
        var showSpendSheet by remember { mutableStateOf(false) }
        var showTransferSheet by remember { mutableStateOf(false) }
        var showLendSheet by remember { mutableStateOf(false) }
        var showLentManagerSheet by remember { mutableStateOf(false) }
        var showSubscriptionSheet by remember { mutableStateOf(false) }
        var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

        LaunchedEffect(pendingAction) {
            when (pendingAction) {
                StashWidgetProvider.ACTION_ADD_INCOME -> {
                    showAddSheet = true
                    showSpendSheet = false
                    showTransferSheet = false
                    showFabMenu = false
                }
                StashWidgetProvider.ACTION_ADD_EXPENSE -> {
                    showSpendSheet = true
                    showAddSheet = false
                    showTransferSheet = false
                    showFabMenu = false
                }
                StashWidgetProvider.ACTION_TRANSFER -> {
                    showTransferSheet = true
                    showAddSheet = false
                    showSpendSheet = false
                    showFabMenu = false
                }
            }
            if (pendingAction != null) {
                onClearPendingAction()
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    StashBottomNavigation(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            val targetIndex = bottomNavScreens.indexOfFirst { it.route == route }
                            val currentTarget = if (pagerState.isScrollInProgress) pagerState.targetPage else pagerState.currentPage
                            if (targetIndex in 0 until bottomNavScreens.size && targetIndex != currentTarget) {
                                showFabMenu = false
                                val pageDiff = kotlin.math.abs(targetIndex - pagerState.currentPage).coerceAtLeast(1)
                                val animDuration = when (pageDiff) {
                                    1 -> 250
                                    2 -> 300
                                    else -> 350
                                }
                                scrollJob?.cancel()
                                scrollJob = coroutineScope.launch {
                                    pagerState.animateScrollToPage(
                                        page = targetIndex,
                                        animationSpec = tween(
                                            durationMillis = animDuration,
                                            easing = FastOutSlowInEasing
                                        )
                                    )
                                }
                            }
                        }
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { showFabMenu = !showFabMenu },
                        shape = CircleShape,
                        containerColor = if (showFabMenu) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.primary,
                        contentColor = if (showFabMenu) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(
                            imageVector = if (showFabMenu) Icons.Rounded.Close else Icons.Rounded.Add,
                            contentDescription = "Quick Action"
                        )
                    }
                }
            ) { innerPadding ->
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = bottomNavScreens.size - 1,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> HomeScreen(
                            innerPadding = innerPadding,
                            transactions = transactions,
                            currencySymbol = settings.currencySymbol,
                            weeklyTrends = weeklyTrends,
                            onSeeAllClick = {
                                showFabMenu = false
                                scrollJob?.cancel()
                                scrollJob = coroutineScope.launch {
                                    pagerState.animateScrollToPage(
                                        page = 1,
                                        animationSpec = tween(
                                            durationMillis = 250,
                                            easing = FastOutSlowInEasing
                                        )
                                    )
                                }
                            },
                            onTransactionClick = { editingTransaction = it },
                            loans = loans,
                            onLentCardClick = { showLentManagerSheet = true },
                            subscriptions = subscriptions,
                            onSubscriptionClick = { showSubscriptionSheet = true }
                        )
                        1 -> HistoryScreen(
                            innerPadding = innerPadding,
                            transactions = transactions,
                            currencySymbol = settings.currencySymbol,
                            onDeleteTransaction = { viewModel.deleteTransaction(it) },
                            onClearAll = { viewModel.clearAllData() },
                            onTransactionClick = { editingTransaction = it }
                        )
                        2 -> TagsScreen(
                            innerPadding = innerPadding,
                            transactions = transactions,
                            categorySummaries = categorySummaries,
                            monthComparison = monthComparison,
                            currencySymbol = settings.currencySymbol,
                            onAddNewTagClick = { showSpendSheet = true }
                        )
                        3 -> SettingsScreen(
                            innerPadding = innerPadding,
                            settings = settings,
                            onThemeChange = { viewModel.setThemeMode(it) },
                            onCurrencyChange = { viewModel.setCurrencySymbol(it) },
                            onBiometricToggle = onBiometricToggle,
                            onExportCsv = onExportCsv,
                            onExportPdf = onExportPdf,
                            onBackupJson = onBackupJson,
                            onRestoreJson = onRestoreJson,
                            onClearAllData = { viewModel.clearAllData() }
                        )
                    }
                }
            }

            // Segmented choice popup above FAB & Scrim
            FabQuickActionMenu(
                visible = showFabMenu,
                onDismiss = { showFabMenu = false },
                onAddMoneyClick = {
                    showFabMenu = false
                    showAddSheet = true
                },
                onSpendMoneyClick = {
                    showFabMenu = false
                    showSpendSheet = true
                },
                onTransferClick = {
                    showFabMenu = false
                    showTransferSheet = true
                },
                onLendMoneyClick = {
                    showFabMenu = false
                    showLendSheet = true
                },
                onSubscriptionsClick = {
                    showFabMenu = false
                    showSubscriptionSheet = true
                }
            )

            // Lend Money Bottom Sheet
            if (showLendSheet) {
                LendMoneyBottomSheet(
                    currencySymbol = settings.currencySymbol,
                    onDismiss = { showLendSheet = false },
                    onConfirmLend = { person, phone, amount, reason, dueDate, mode ->
                        viewModel.lendMoney(person, phone, amount, reason, dueDate, mode)
                    }
                )
            }

            // Lent Money Manager Sheet
            if (showLentManagerSheet) {
                LentMoneyManagerSheet(
                    loans = loans,
                    currencySymbol = settings.currencySymbol,
                    onDismiss = { showLentManagerSheet = false },
                    onAddNewLoanClick = { showLendSheet = true },
                    onRecordRepayment = { loan, amount, mode, note ->
                        viewModel.recordRepayment(loan, amount, mode, note)
                    },
                    onAddMoreToLoan = { loan, amount, mode, note ->
                        viewModel.addMoreToLoan(loan, amount, mode, note)
                    },
                    onDeleteLoan = { loan ->
                        viewModel.deleteLoan(loan)
                    }
                )
            }

            // Add Money Bottom Sheet
            if (showAddSheet) {
                AddMoneyBottomSheet(
                    currencySymbol = settings.currencySymbol,
                    onDismiss = { showAddSheet = false },
                    onSave = { type, amount, source, note, timestamp, paymentMode ->
                        viewModel.addTransaction(type, amount, source, note, timestamp, paymentMode)
                    }
                )
            }

            // Spend Money Bottom Sheet
            if (showSpendSheet) {
                SpendMoneyBottomSheet(
                    currencySymbol = settings.currencySymbol,
                    onDismiss = { showSpendSheet = false },
                    onSave = { type, amount, tag, note, timestamp, paymentMode ->
                        viewModel.addTransaction(type, amount, tag, note, timestamp, paymentMode)
                    }
                )
            }

            // Transfer Money Bottom Sheet
            if (showTransferSheet) {
                TransferMoneyBottomSheet(
                    currencySymbol = settings.currencySymbol,
                    onDismiss = { showTransferSheet = false },
                    onConfirmTransfer = { amount, fromMode, note ->
                        viewModel.addTransfer(amount, fromMode, note)
                    }
                )
            }

            // Subscription Manager Sheet
            if (showSubscriptionSheet) {
                SubscriptionManagerSheet(
                    subscriptions = subscriptions,
                    currencySymbol = settings.currencySymbol,
                    onDismiss = { showSubscriptionSheet = false },
                    onAddSubscription = { name, amount, category, mode, dueDay ->
                        viewModel.addSubscription(name, amount, category, mode, dueDay)
                    },
                    onDeleteSubscription = { sub ->
                        viewModel.deleteSubscription(sub)
                    },
                    onLogPayment = { sub ->
                        viewModel.logSubscriptionPayment(sub)
                    }
                )
            }

            // Edit Transaction Bottom Sheet
            editingTransaction?.let { txToEdit ->
                EditTransactionBottomSheet(
                    transaction = txToEdit,
                    currencySymbol = settings.currencySymbol,
                    onDismiss = { editingTransaction = null },
                    onSave = { updated ->
                        viewModel.updateTransaction(updated)
                        editingTransaction = null
                    },
                    onDelete = { toDelete ->
                        viewModel.deleteTransaction(toDelete)
                        editingTransaction = null
                    }
                )
            }
        }
    }
}
