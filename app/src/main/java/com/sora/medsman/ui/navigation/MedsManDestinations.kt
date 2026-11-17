package com.sora.medsman.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Type-safe Navigation 3 destinations for the three top-level tabs. */
@Serializable
data object DashboardRoute : NavKey

@Serializable
data object InventoryRoute : NavKey

@Serializable
data object HistoryRoute : NavKey

data class TopLevelDestination(
    val route: NavKey,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val topLevelDestinations = listOf(
    TopLevelDestination(
        route = DashboardRoute,
        label = "Today",
        selectedIcon = Icons.Filled.WbSunny,
        unselectedIcon = Icons.Outlined.WbSunny
    ),
    TopLevelDestination(
        route = InventoryRoute,
        label = "Inventory",
        selectedIcon = Icons.Filled.Medication,
        unselectedIcon = Icons.Outlined.Medication
    ),
    TopLevelDestination(
        route = HistoryRoute,
        label = "History",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History
    )
)
