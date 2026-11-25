package com.nan.stash.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nan.stash.data.local.ThemeMode
import com.nan.stash.ui.components.sanitizeDecimalInput
import com.nan.stash.ui.util.StashHaptics
import java.util.Locale

@Composable
fun SettingsHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Preferences, security, and offline data",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SettingsCardHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    iconContainerColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable {
                StashHaptics.keyTap(haptic)
                onCheckedChange(!checked)
            }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = {
                StashHaptics.keyTap(haptic)
                onCheckedChange(it)
            }
        )
    }
}

@Composable
fun SettingsActionRow(
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    iconContainerColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable {
                StashHaptics.buttonClick(haptic)
                onClick()
            }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun PreferencesSettingsCard(
    themeMode: ThemeMode,
    dynamicColor: Boolean,
    currencySymbol: String,
    onThemeChange: (ThemeMode) -> Unit,
    onDynamicColorToggle: (Boolean) -> Unit,
    onCurrencyChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            SettingsCardHeader(icon = Icons.Rounded.Tune, title = "PREFERENCES")

            Spacer(modifier = Modifier.height(14.dp))

            // Theme Preference
            Text(
                text = "Theme Appearance",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Choose light, dark, or system default",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            val themeOptions = listOf(ThemeMode.SYSTEM to "System", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                themeOptions.forEachIndexed { index, (mode, label) ->
                    SegmentedButton(
                        selected = themeMode == mode,
                        onClick = {
                            StashHaptics.keyTap(haptic)
                            onThemeChange(mode)
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = themeOptions.size)
                    ) {
                        Text(label)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(6.dp))

            // Dynamic Monet Color Switch
            SettingsSwitchRow(
                icon = Icons.Rounded.ColorLens,
                iconTint = MaterialTheme.colorScheme.primary,
                iconContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                title = "Dynamic Color (Monet)",
                subtitle = "Sample accent tones from wallpaper (Android 12+)",
                checked = dynamicColor,
                onCheckedChange = onDynamicColorToggle
            )

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(12.dp))

            // Currency
            Text(
                text = "Default Currency",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Applied to records, reports, and balance summaries",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            val currencyList = listOf("$" to "USD ($)", "€" to "EUR (€)", "£" to "GBP (£)", "₹" to "INR (₹)", "¥" to "JPY (¥)")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                currencyList.forEach { (sym, _) ->
                    FilterChip(
                        selected = currencySymbol == sym,
                        onClick = {
                            StashHaptics.keyTap(haptic)
                            onCurrencyChange(sym)
                        },
                        label = { Text(sym, fontWeight = FontWeight.Bold) },
                        shape = CircleShape
                    )
                }
            }
        }
    }
}

@Composable
fun SecurityAlertsSettingsCard(
    isBiometricEnabled: Boolean,
    lowBalanceWarningEnabled: Boolean,
    lowOnlineThreshold: Double,
    lowCashThreshold: Double,
    currencySymbol: String,
    onBiometricToggle: (Boolean) -> Unit,
    onLowBalanceWarningToggle: (Boolean) -> Unit,
    onOnlineThresholdChange: (Double) -> Unit,
    onCashThresholdChange: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            SettingsCardHeader(icon = Icons.Rounded.Shield, title = "SECURITY & ALERTS")

            Spacer(modifier = Modifier.height(10.dp))

            // Biometric Switch
            SettingsSwitchRow(
                icon = Icons.Rounded.Fingerprint,
                iconTint = MaterialTheme.colorScheme.primary,
                iconContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                title = "Biometric Lock",
                subtitle = "Require fingerprint or screen lock to open Stash",
                checked = isBiometricEnabled,
                onCheckedChange = onBiometricToggle
            )

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(6.dp))

            // Low Balance Alert Switch
            SettingsSwitchRow(
                icon = Icons.Rounded.NotificationsActive,
                iconTint = MaterialTheme.colorScheme.secondary,
                iconContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                title = "Low Balance Alerts",
                subtitle = "Highlight wallets and warn before expenses exceed limit",
                checked = lowBalanceWarningEnabled,
                onCheckedChange = onLowBalanceWarningToggle
            )

            // Animated expansion for threshold inputs
            AnimatedVisibility(
                visible = lowBalanceWarningEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)) {
                    ThresholdInputSection(
                        title = "Online Money Alert Threshold",
                        subtitle = "Trigger alert when Online Money falls to or below this amount",
                        threshold = lowOnlineThreshold,
                        presets = listOf(25.0, 50.0, 100.0, 250.0, 500.0),
                        currencySymbol = currencySymbol,
                        onThresholdChange = onOnlineThresholdChange
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    Spacer(modifier = Modifier.height(14.dp))

                    ThresholdInputSection(
                        title = "Cash Available Alert Threshold",
                        subtitle = "Trigger alert when Cash Available falls to or below this amount",
                        threshold = lowCashThreshold,
                        presets = listOf(10.0, 20.0, 50.0, 100.0, 200.0),
                        currencySymbol = currencySymbol,
                        onThresholdChange = onCashThresholdChange
                    )
                }
            }
        }
    }
}

private fun formatThresholdText(value: Double): String {
    return if (value > 0.0) String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.') else ""
}

@Composable
private fun ThresholdInputSection(
    title: String,
    subtitle: String,
    threshold: Double,
    presets: List<Double>,
    currencySymbol: String,
    onThresholdChange: (Double) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var inputText by remember { mutableStateOf(formatThresholdText(threshold)) }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(threshold) {
        if (!isFocused) {
            inputText = formatThresholdText(threshold)
        }
    }

    Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = inputText,
        onValueChange = { input ->
            val clean = sanitizeDecimalInput(input)
            if (clean != null) {
                inputText = clean
                onThresholdChange(clean.toDoubleOrNull() ?: 0.0)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (!focusState.isFocused) {
                    inputText = formatThresholdText(threshold)
                }
            },
        placeholder = { Text("0.00") },
        prefix = { Text("$currencySymbol ") },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        presets.forEach { preset ->
            FilterChip(
                selected = threshold == preset,
                onClick = {
                    StashHaptics.keyTap(haptic)
                    onThresholdChange(preset)
                    inputText = String.format(Locale.US, "%.0f", preset)
                },
                label = { Text("$currencySymbol${preset.toInt()}", fontWeight = FontWeight.SemiBold) },
                shape = CircleShape
            )
        }
    }
}

@Composable
fun DataManagementSettingsCard(
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit,
    onBackupJson: () -> Unit,
    onRestoreJson: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            SettingsCardHeader(icon = Icons.Rounded.Storage, title = "DATA MANAGEMENT")

            Spacer(modifier = Modifier.height(10.dp))

            // PDF
            SettingsActionRow(
                icon = Icons.Rounded.PictureAsPdf,
                iconTint = MaterialTheme.colorScheme.error,
                iconContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                title = "Printable PDF Statement",
                subtitle = "Generate formatted report for sharing or printing",
                onClick = onExportPdf
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // CSV
            SettingsActionRow(
                icon = Icons.Rounded.TableChart,
                iconTint = MaterialTheme.colorScheme.primary,
                iconContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                title = "Export to CSV",
                subtitle = "Download spreadsheet for Excel or Google Sheets",
                onClick = onExportCsv
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // JSON Backup
            SettingsActionRow(
                icon = Icons.Rounded.FileDownload,
                iconTint = MaterialTheme.colorScheme.secondary,
                iconContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                title = "Backup Database (JSON)",
                subtitle = "Save complete snapshot to device Downloads",
                onClick = onBackupJson
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // JSON Restore
            SettingsActionRow(
                icon = Icons.Rounded.FileUpload,
                iconTint = MaterialTheme.colorScheme.tertiary,
                iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                title = "Restore Backup (JSON)",
                subtitle = "Import saved transactions, loans, and bills",
                onClick = onRestoreJson
            )
        }
    }
}

@Composable
fun AboutAndPrivacySettingsCard(
    appVersionName: String,
    onClearAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            SettingsCardHeader(icon = Icons.Rounded.Info, title = "ABOUT & PRIVACY")

            Spacer(modifier = Modifier.height(14.dp))

            // App details
            Text(
                text = "Stash v$appVersionName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "100% offline personal finance and peer-to-peer loan tracker built with Material 3 and Jetpack Compose.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Target SDK 35 (Android 15) • Edge-to-Edge Enabled",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(16.dp))

            // Differentiated Destructive Action Container
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable {
                            StashHaptics.buttonClick(haptic)
                            onClearAllClick()
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteForever,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Reset All Data",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Permanently wipe all transactions, loans, and subscriptions on this device",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
