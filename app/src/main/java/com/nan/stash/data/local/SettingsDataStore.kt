package com.nan.stash.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "stash_settings")

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

data class StashSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val currencySymbol: String = "$",
    val isBiometricEnabled: Boolean = false,
    val lowBalanceWarningEnabled: Boolean = false,
    val lowOnlineThreshold: Double = 0.0,
    val lowCashThreshold: Double = 0.0
)

class SettingsDataStore(private val context: Context) {

    private val securityPrefs = context.getSharedPreferences("stash_security", Context.MODE_PRIVATE)

    fun isBiometricEnabledSync(): Boolean {
        return securityPrefs.getBoolean("biometric_enabled", false)
    }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val LOW_BALANCE_WARNING_ENABLED = booleanPreferencesKey("low_balance_warning_enabled")
        val LOW_ONLINE_THRESHOLD = doublePreferencesKey("low_online_threshold")
        val LOW_CASH_THRESHOLD = doublePreferencesKey("low_cash_threshold")
    }

    val settingsFlow: Flow<StashSettings> = context.dataStore.data.map { preferences ->
        val themeString = preferences[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        val themeMode = try {
            ThemeMode.valueOf(themeString)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }

        val bioEnabled = preferences[Keys.BIOMETRIC_ENABLED] ?: isBiometricEnabledSync()
        if (securityPrefs.getBoolean("biometric_enabled", false) != bioEnabled) {
            securityPrefs.edit().putBoolean("biometric_enabled", bioEnabled).apply()
        }

        StashSettings(
            themeMode = themeMode,
            dynamicColor = preferences[Keys.DYNAMIC_COLOR] ?: true,
            currencySymbol = preferences[Keys.CURRENCY_SYMBOL] ?: "$",
            isBiometricEnabled = bioEnabled,
            lowBalanceWarningEnabled = preferences[Keys.LOW_BALANCE_WARNING_ENABLED] ?: false,
            lowOnlineThreshold = preferences[Keys.LOW_ONLINE_THRESHOLD] ?: 0.0,
            lowCashThreshold = preferences[Keys.LOW_CASH_THRESHOLD] ?: 0.0
        )
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = themeMode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setCurrencySymbol(symbol: String) {
        context.dataStore.edit { it[Keys.CURRENCY_SYMBOL] = symbol }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        securityPrefs.edit().putBoolean("biometric_enabled", enabled).apply()
        context.dataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setLowBalanceWarningEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.LOW_BALANCE_WARNING_ENABLED] = enabled }
    }

    suspend fun setLowOnlineThreshold(amount: Double) {
        context.dataStore.edit { it[Keys.LOW_ONLINE_THRESHOLD] = amount }
    }

    suspend fun setLowCashThreshold(amount: Double) {
        context.dataStore.edit { it[Keys.LOW_CASH_THRESHOLD] = amount }
    }
}
