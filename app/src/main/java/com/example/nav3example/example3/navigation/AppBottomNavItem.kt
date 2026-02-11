package com.example.nav3example.example3.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

data class AppBottomNavItem(
    val icon: ImageVector,
    val title: String,
)

val TOP_LEVEL_DESTINATIONS = mapOf(
    AppRoute3.Todo to AppBottomNavItem(
        icon = Icons.Outlined.Home,
        title = "Home"
    ),
    AppRoute3.Favorites to AppBottomNavItem(
        icon = Icons.Outlined.Favorite,
        title = "Favorites"
    ),
    AppRoute3.Settings to AppBottomNavItem(
        icon = Icons.Outlined.Settings,
        title = "Settings"
    )
)