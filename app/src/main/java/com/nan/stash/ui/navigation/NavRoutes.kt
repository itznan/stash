package com.nan.stash.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("home", "Home", Icons.Rounded.Home)
    data object History : Screen("history", "History", Icons.AutoMirrored.Rounded.ReceiptLong)
    data object Tags : Screen("tags", "Tags", Icons.Rounded.PieChart)
    data object Settings : Screen("settings", "Settings", Icons.Rounded.Settings)
}

val bottomNavScreens = listOf(
    Screen.Home,
    Screen.History,
    Screen.Tags,
    Screen.Settings
)
