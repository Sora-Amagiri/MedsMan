package com.sora.medsman.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.sora.medsman.data.repository.MedsManRepository
import com.sora.medsman.ui.navigation.DashboardRoute
import com.sora.medsman.ui.navigation.HistoryRoute
import com.sora.medsman.ui.navigation.InventoryRoute
import com.sora.medsman.ui.navigation.topLevelDestinations
import com.sora.medsman.ui.screens.DashboardScreen
import com.sora.medsman.ui.screens.HistoryScreen
import com.sora.medsman.ui.screens.InventoryScreen
import com.sora.medsman.ui.viewmodel.HistoryViewModel
import com.sora.medsman.ui.viewmodel.InventoryViewModel
import com.sora.medsman.ui.viewmodel.MedsManMainViewModel
import com.sora.medsman.ui.viewmodel.ProfileViewModel
import com.sora.medsman.ui.viewmodel.ScheduleViewModel

/**
 * Application shell that adapts the top-level navigation to the window size:
 * a bottom navigation bar on compact screens, a navigation rail on medium
 * screens, and a permanent navigation drawer on expanded screens.
 */
@Composable
fun MedsManApp(
    repository: MedsManRepository,
    modifier: Modifier = Modifier
) {
    val backStack = rememberNavBackStack(DashboardRoute)
    val currentRoute = backStack.lastOrNull() ?: DashboardRoute

    val mainViewModel: MedsManMainViewModel = viewModel(
        factory = MedsManMainViewModel.Factory(repository)
    )
    val scheduleViewModel: ScheduleViewModel = viewModel(
        factory = ScheduleViewModel.Factory(repository)
    )
    val profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.Factory(repository)
    )
    val inventoryViewModel: InventoryViewModel = viewModel(
        factory = InventoryViewModel.Factory(repository)
    )
    val historyViewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModel.Factory(repository)
    )

    val destinations = remember { topLevelDestinations }

    val selectTopLevel: (NavKey) -> Unit = { route ->
        // Keep Today as the root beneath a secondary tab so system back returns
        // to the dashboard instead of exiting the app.
        backStack.clear()
        if (route != DashboardRoute) {
            backStack.add(DashboardRoute)
        }
        backStack.add(route)
    }

    NavigationSuiteScaffold(
        modifier = modifier,
        navigationSuiteItems = {
            destinations.forEach { destination ->
                val isSelected = currentRoute == destination.route
                item(
                    selected = isSelected,
                    onClick = { selectTopLevel(destination.route) },
                    icon = {
                        Icon(
                            imageVector = if (isSelected) {
                                destination.selectedIcon
                            } else {
                                destination.unselectedIcon
                            },
                            contentDescription = stringResource(destination.labelRes)
                        )
                    },
                    label = { Text(stringResource(destination.labelRes)) }
                )
            }
        }
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = {
                if (backStack.size > 1) {
                    backStack.removeAt(backStack.lastIndex)
                }
            },
            entryProvider = { key ->
                when (key) {
                    DashboardRoute -> NavEntry(key) {
                        DashboardScreen(
                            mainViewModel = mainViewModel,
                            scheduleViewModel = scheduleViewModel,
                            profileViewModel = profileViewModel,
                            onNavigateToInventory = { selectTopLevel(InventoryRoute) }
                        )
                    }

                    InventoryRoute -> NavEntry(key) {
                        InventoryScreen(viewModel = inventoryViewModel)
                    }

                    HistoryRoute -> NavEntry(key) {
                        HistoryScreen(viewModel = historyViewModel)
                    }

                    else -> error("Unknown destination: $key")
                }
            }
        )
    }
}
