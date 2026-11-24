package com.sora.medsman.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.sora.medsman.R
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
    @StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val topLevelDestinations = listOf(
    TopLevelDestination(
        route = DashboardRoute,
        labelRes = R.string.nav_today,
        selectedIcon = Icons.Filled.WbSunny,
        unselectedIcon = Icons.Outlined.WbSunny
    ),
    TopLevelDestination(
        route = InventoryRoute,
        labelRes = R.string.nav_inventory,
        selectedIcon = Icons.Filled.Medication,
        unselectedIcon = Icons.Outlined.Medication
    ),
    TopLevelDestination(
        route = HistoryRoute,
        labelRes = R.string.nav_history,
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History
    )
)
