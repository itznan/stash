package com.nan.stash

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.nan.stash.ui.MainViewModel
import com.nan.stash.ui.StashApp
import com.nan.stash.ui.components.BiometricLockScreen
import com.nan.stash.ui.theme.StashTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var pendingAction by mutableStateOf<String?>(null)
    private var isAppUnlocked by mutableStateOf(false)
    private var isPromptShowing = false
    private var isLaunchingPicker = false

    private fun updateSecurityFlags(isBiometricEnabled: Boolean) {
        if (isBiometricEnabled) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingAction = intent.action
    }

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

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
        try {
            super.startActivityForResult(intent, requestCode, options)
        } catch (e: IllegalArgumentException) {
            if (e.message?.contains("16 bits") == true) {
                super.startActivityForResult(intent, requestCode and 0xFFFF, options)
            } else {
                throw e
            }
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                params.setFrameRateBoostOnTouchEnabled(true)
            }
            window.attributes = params
        }

        super.onCreate(savedInstanceState)

        if (pendingAction == null && intent?.action != null) {
            pendingAction = intent.action
        }

        // Initialize lock state synchronously from disk
        val isBioEnabled = viewModel.isBiometricEnabledSync()
        isAppUnlocked = !isBioEnabled
        updateSecurityFlags(isBioEnabled)

        // Reactively observe settings changes (e.g. if disabled in settings)
        lifecycleScope.launch {
            viewModel.settings.collect { settings ->
                updateSecurityFlags(settings.isBiometricEnabled)
                if (!settings.isBiometricEnabled) {
                    isAppUnlocked = true
                }
            }
        }

        setContent {
            val settings by viewModel.settings.collectAsState()

            val restoreLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val content = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                            viewModel.restoreFromJson(content) { success, count ->
                                runOnUiThread {
                                    if (success) {
                                        Toast.makeText(this@MainActivity, "Restored $count records from backup!", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(this@MainActivity, "Failed: invalid backup file", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "Restore error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
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

            val onExportPdf: () -> Unit = {
                try {
                    val transactions = viewModel.transactions.value
                    val file = viewModel.exportToPdf(transactions, settings.currencySymbol)
                    Toast.makeText(this@MainActivity, "PDF generated: ${file.name}", Toast.LENGTH_LONG).show()
                    val uri = FileProvider.getUriForFile(this@MainActivity, "com.nan.stash.provider", file)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    isLaunchingPicker = true
                    startActivity(Intent.createChooser(shareIntent, "Share Stash PDF Statement"))
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "PDF export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            val onExportCsv: () -> Unit = {
                try {
                    val file = viewModel.exportToCsv(viewModel.transactions.value)
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
                    val file = viewModel.createBackupJson()
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
                StashTheme {
                    BiometricLockScreen(
                        onUnlockClick = { promptBiometric() }
                    )
                }
            } else {
                StashApp(
                    viewModel = viewModel,
                    pendingAction = pendingAction,
                    onClearPendingAction = { pendingAction = null },
                    onBiometricToggle = onBiometricToggle,
                    onExportCsv = onExportCsv,
                    onExportPdf = onExportPdf,
                    onBackupJson = onBackupJson,
                    onRestoreJson = {
                        try {
                            isLaunchingPicker = true
                            restoreLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                        } catch (e: Exception) {
                            isLaunchingPicker = false
                            Toast.makeText(this@MainActivity, "Could not open file picker: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}
