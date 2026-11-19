package com.nan.stash.ui.screens.settings

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nan.stash.data.local.StashSettings
import com.nan.stash.data.local.ThemeMode

@Composable
fun SettingsScreen(
    innerPadding: PaddingValues,
    settings: StashSettings,
    onThemeChange: (ThemeMode) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
    onBackupJson: () -> Unit,
    onRestoreJson: () -> Unit,
    onClearAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appVersionName = remember(context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                ).versionName ?: "1.0.2"
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.2"
            }
        } catch (_: Exception) {
            "1.0.2"
        }
    }
    var showClearDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .consumeWindowInsets(innerPadding),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = innerPadding.calculateTopPadding() + 8.dp,
            bottom = innerPadding.calculateBottomPadding() + 88.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Appearance Card
        item {
            AppearanceSettingsCard(
                themeMode = settings.themeMode,
                onThemeChange = onThemeChange
            )
        }

        // Security & Biometric Card
        item {
            SecuritySettingsCard(
                isBiometricEnabled = settings.isBiometricEnabled,
                onBiometricToggle = onBiometricToggle
            )
        }

        // Currency Card
        item {
            CurrencySettingsCard(
                currencySymbol = settings.currencySymbol,
                onCurrencyChange = onCurrencyChange
            )
        }

        // Backup & Export Card
        item {
            BackupExportSettingsCard(
                onExportPdf = onExportPdf,
                onExportCsv = onExportCsv,
                onBackupJson = onBackupJson,
                onRestoreJson = onRestoreJson
            )
        }

        // Data & Privacy Card
        item {
            DataPrivacySettingsCard(
                onClearAllClick = { showClearDialog = true }
            )
        }

        // About Card
        item {
            AboutSettingsCard(
                appVersionName = appVersionName
            )
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAllData()
                        showClearDialog = false
                    }
                ) {
                    Text("Reset All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Reset All Data?") },
            text = { Text("This will permanently delete all recorded transactions from this device.") }
        )
    }
}
