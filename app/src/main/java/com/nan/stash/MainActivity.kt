package com.nan.stash

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import com.nan.stash.ui.navigation.bottomNavScreens
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nan.stash.data.local.ThemeMode
import com.nan.stash.data.local.TransactionEntity
import com.nan.stash.ui.MainViewModel
import com.nan.stash.ui.navigation.Screen
import com.nan.stash.ui.navigation.StashBottomNavigation
import com.nan.stash.ui.screens.AddMoneyBottomSheet
import com.nan.stash.ui.screens.EditTransactionBottomSheet
import com.nan.stash.ui.screens.HistoryScreen
import com.nan.stash.ui.screens.HomeScreen
import com.nan.stash.ui.screens.LendMoneyBottomSheet
import com.nan.stash.ui.screens.LentMoneyManagerSheet
import com.nan.stash.ui.screens.SettingsScreen
import com.nan.stash.ui.screens.SpendMoneyBottomSheet
import com.nan.stash.ui.screens.TagsScreen
import com.nan.stash.ui.screens.TransferMoneyBottomSheet
import androidx.compose.material.icons.rounded.SwapHoriz
import com.nan.stash.ui.theme.StashTheme
import com.nan.stash.ui.theme.incomeColor
import com.nan.stash.ui.theme.spendingColor

class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var isAppUnlocked by mutableStateOf(false)
    private var isPromptShowing = false
    private var isLaunchingPicker = false

    fun promptBiometric() {
        val isBioEnabled = viewModel.isBiometricEnabledSync()
        if (!isBioEnabled || isAppUnlocked || isPromptShowing) return

        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(authenticators)

        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, "Set up screen lock or fingerprint in Android settings to use this feature", Toast.LENGTH_LONG).show()
            isAppUnlocked = true
            return
        }

        try {
            isPromptShowing = true
            val executor = ContextCompat.getMainExecutor(this)
            val prompt = BiometricPrompt(
                this,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        isPromptShowing = false
                        isAppUnlocked = true
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        isPromptShowing = false
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Stash")
                .setSubtitle("Confirm your fingerprint or screen lock")
                .setAllowedAuthenticators(authenticators)
                .build()

            prompt.authenticate(promptInfo)
        } catch (_: Exception) {
            isPromptShowing = false
        }
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.isBiometricEnabledSync() && !isAppUnlocked && !isPromptShowing) {
            promptBiometric()
        }
    }

    override fun onStop() {
        super.onStop()
        if (isLaunchingPicker) {
            isLaunchingPicker = false
        } else if (viewModel.isBiometricEnabledSync()) {
            isAppUnlocked = false
            isPromptShowing = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Step 1: Android Edge-to-Edge compliance
        enableEdgeToEdge()

        // Step 2: 120Hz / High Refresh Rate & Low Latency Display Mode
        window.setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val params = window.attributes
            val display = display
            val modes = display?.supportedModes ?: emptyArray()
            val maxMode = modes.maxByOrNull { it.refreshRate }
            if (maxMode != null) {
                params.preferredDisplayModeId = maxMode.modeId
                params.preferredRefreshRate = maxMode.refreshRate.coerceAtLeast(120f)
            } else {
                params.preferredRefreshRate = 120f
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                params.setFrameRateBoostOnTouchEnabled(true)
            }
            window.attributes = params
        }

        super.onCreate(savedInstanceState)

        // Initialize lock state synchronously from disk
        val isBioEnabled = viewModel.isBiometricEnabledSync()
        isAppUnlocked = !isBioEnabled

        // Reactively observe settings changes (e.g. if disabled in settings)
        lifecycleScope.launch {
            viewModel.settings.collect { settings ->
                if (!settings.isBiometricEnabled) {
                    isAppUnlocked = true
                }
            }
        }

        setContent {
            val settings by viewModel.settings.collectAsState()
            val transactions by viewModel.transactions.collectAsState()
            val loans by viewModel.loans.collectAsState()

            val isDarkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            StashTheme(
                darkTheme = isDarkTheme
            ) {
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
                var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

                val promptBiometricAction = {
                    promptBiometric()
                }

                val weeklyTrends by viewModel.weeklyTrends.collectAsState()
                val categorySummaries by viewModel.categorySummaries.collectAsState()
                val monthComparison by viewModel.monthComparison.collectAsState()

                val restoreLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    if (uri != null) {
                        try {
                            val content = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                            viewModel.restoreFromJson(content) { success, count ->
                                if (success) {
                                    Toast.makeText(this@MainActivity, "Restored $count transactions from backup!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(this@MainActivity, "Failed: invalid backup file", Toast.LENGTH_LONG).show()
                                }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@MainActivity, "Restore error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                val onBiometricToggle: (Boolean) -> Unit = { enable ->
                    if (enable) {
                        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                BiometricManager.Authenticators.DEVICE_CREDENTIAL
                        val canAuth = BiometricManager.from(this@MainActivity).canAuthenticate(authenticators)
                        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
                            Toast.makeText(this@MainActivity, "Please set up a fingerprint or screen lock in device settings first", Toast.LENGTH_LONG).show()
                        } else {
                            try {
                                val executor = ContextCompat.getMainExecutor(this@MainActivity)
                                val prompt = BiometricPrompt(
                                    this@MainActivity,
                                    executor,
                                    object : BiometricPrompt.AuthenticationCallback() {
                                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                            super.onAuthenticationSucceeded(result)
                                            viewModel.setBiometricEnabled(true)
                                            isAppUnlocked = true
                                            Toast.makeText(this@MainActivity, "Biometric Lock enabled", Toast.LENGTH_SHORT).show()
                                        }
                                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                            super.onAuthenticationError(errorCode, errString)
                                            Toast.makeText(this@MainActivity, "Could not enable: $errString", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                                    .setTitle("Enable Biometric Lock")
                                    .setSubtitle("Confirm your fingerprint or screen lock")
                                    .setAllowedAuthenticators(authenticators)
                                    .build()
                                prompt.authenticate(promptInfo)
                            } catch (e: Exception) {
                                Toast.makeText(this@MainActivity, "Biometrics not available: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        viewModel.setBiometricEnabled(false)
                        isAppUnlocked = true
                        Toast.makeText(this@MainActivity, "Biometric Lock disabled", Toast.LENGTH_SHORT).show()
                    }
                }

                val onExportCsv: () -> Unit = {
                    try {
                        val file = viewModel.exportToCsv(transactions)
                        Toast.makeText(this@MainActivity, "Exported: ${file.name}", Toast.LENGTH_LONG).show()
                        val uri = FileProvider.getUriForFile(this@MainActivity, "com.nan.stash.provider", file)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        isLaunchingPicker = true
                        startActivity(Intent.createChooser(shareIntent, "Share Stash CSV"))
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                val onBackupJson: () -> Unit = {
                    try {
                        val file = viewModel.createBackupJson(transactions)
                        Toast.makeText(this@MainActivity, "Backup saved: ${file.name}", Toast.LENGTH_LONG).show()
                        val uri = FileProvider.getUriForFile(this@MainActivity, "com.nan.stash.provider", file)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        isLaunchingPicker = true
                        startActivity(Intent.createChooser(shareIntent, "Share Stash Backup"))
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "Backup failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                if (settings.isBiometricEnabled && !isAppUnlocked) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Fingerprint,
                                    contentDescription = "Locked",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(48.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "Stash is Locked",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Verify your fingerprint or screen lock to access your data",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            Button(
                                onClick = { promptBiometric() },
                                shape = CircleShape,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(imageVector = Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Unlock with Biometrics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
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
                                    onLentCardClick = { showLentManagerSheet = true }
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
                                    onAddNewTagClick = {
                                        showSpendSheet = true
                                    }
                                )
                                3 -> SettingsScreen(
                                    innerPadding = innerPadding,
                                    settings = settings,
                                    onThemeChange = { viewModel.setThemeMode(it) },
                                    onCurrencyChange = { viewModel.setCurrencySymbol(it) },
                                    onBiometricToggle = onBiometricToggle,
                                    onExportCsv = onExportCsv,
                                    onBackupJson = onBackupJson,
                                    onRestoreJson = {
                                        isLaunchingPicker = true
                                        restoreLauncher.launch("*/*")
                                    },
                                    onClearAllData = { viewModel.clearAllData() }
                                )
                            }
                        }
                    }

                    // Scrim when FAB menu is open
                    if (showFabMenu) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f))
                                .clickable { showFabMenu = false }
                        )
                    }

                    // Segmented choice popup above FAB
                    AnimatedVisibility(
                        visible = showFabMenu,
                        enter = fadeIn() + slideInVertically { it / 2 },
                        exit = fadeOut() + slideOutVertically { it / 2 },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 96.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shadowElevation = 6.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Add Money Option
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.incomeColor.copy(alpha = 0.15f))
                                        .clickable {
                                            showFabMenu = false
                                            showAddSheet = true
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.incomeColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ArrowUpward,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Add Money",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.incomeColor
                                    )
                                }

                                // Spend Money Option
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.spendingColor.copy(alpha = 0.15f))
                                        .clickable {
                                            showFabMenu = false
                                            showSpendSheet = true
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.spendingColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ArrowDownward,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Spend Money",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.spendingColor
                                    )
                                }

                                // Transfer Option (Online ⇄ Cash)
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
                                        .clickable {
                                            showFabMenu = false
                                            showTransferSheet = true
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.tertiary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.SwapHoriz,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Transfer",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }

                                // Lend Money Option
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                                        .clickable {
                                            showFabMenu = false
                                            showLendSheet = true
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Lend Money",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }

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
                            onAddNewLoanClick = {
                                showLendSheet = true
                            },
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
    }
}
}
