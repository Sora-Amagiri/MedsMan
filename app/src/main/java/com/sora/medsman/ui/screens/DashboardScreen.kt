package com.sora.medsman.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sora.medsman.R
import com.sora.medsman.data.model.TakeStatus
import com.sora.medsman.data.model.UserProfile
import com.sora.medsman.ui.components.AboutSheet
import com.sora.medsman.ui.components.DosageScheduleCard
import com.sora.medsman.ui.components.NatureHeaderCard
import com.sora.medsman.ui.components.UserProfileSheet
import com.sora.medsman.ui.theme.MedsManTheme
import com.sora.medsman.ui.viewmodel.*
import java.util.Calendar

private fun todayTimeMillis(timeOfDay: String): Long {
    val parts = timeOfDay.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

enum class ScheduleFilter(val label: String) {
    ALL("All Today"),
    PENDING("Pending"),
    TAKEN("Taken"),
    MISSED("Missed"),
    SKIPPED("Skipped")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    mainViewModel: MedsManMainViewModel,
    scheduleViewModel: ScheduleViewModel,
    profileViewModel: ProfileViewModel,
    modifier: Modifier = Modifier,
    onNavigateToInventory: () -> Unit = {}
) {
    val dashboardState by mainViewModel.uiState.collectAsState()
    val scheduleState by scheduleViewModel.uiState.collectAsState()
    val profileState by profileViewModel.uiState.collectAsState()
    val context = LocalContext.current

    var selectedFilter by rememberSaveable { mutableStateOf(ScheduleFilter.ALL) }
    var showProfileSheet by rememberSaveable { mutableStateOf(false) }
    var showAboutSheet by rememberSaveable { mutableStateOf(false) }

    val userProfile = (profileState as? ProfileUiState.Success)?.userProfile ?: UserProfile()
    val dashboardSummary = (dashboardState as? DashboardUiState.Success)?.summary ?: DashboardSummary()
    val todaySchedules = (scheduleState as? ScheduleUiState.Success)?.todaySchedules ?: emptyList()

    val filteredSchedules = remember(todaySchedules, selectedFilter) {
        when (selectedFilter) {
            ScheduleFilter.ALL -> todaySchedules
            ScheduleFilter.PENDING -> todaySchedules.filter { it.todayStatus == null }
            ScheduleFilter.TAKEN -> todaySchedules.filter { it.todayStatus == TakeStatus.TAKEN }
            ScheduleFilter.MISSED -> todaySchedules.filter { it.todayStatus == TakeStatus.MISSED }
            ScheduleFilter.SKIPPED -> todaySchedules.filter { it.todayStatus == TakeStatus.SKIPPED }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SelfImprovement,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAboutSheet = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.about_title),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showProfileSheet = true }) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "User Profile",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Nature Canvas Header Card
            item {
                NatureHeaderCard(
                    userName = userProfile.name.ifBlank { "Friend" },
                    takenCount = dashboardSummary.todayTakenCount,
                    totalScheduledCount = dashboardSummary.todayScheduledCount,
                    lowStockCount = dashboardSummary.lowStockAlertCount,
                    onProfileClick = { showProfileSheet = true },
                    onLowStockClick = onNavigateToInventory
                )
            }

            // Schedule Filter Chips Section Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily Schedule",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        Text(
                            text = "${filteredSchedules.size} item(s)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Filter Chips Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(ScheduleFilter.entries.toTypedArray()) { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter.label) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            }

            // List of Dosage Schedule Cards
            if (filteredSchedules.isEmpty()) {
                item {
                    EmptyScheduleState(selectedFilter = selectedFilter)
                }
            } else {
                items(
                    items = filteredSchedules,
                    key = { "${it.schedule.id}_${it.schedule.medicationId}_${it.schedule.timeOfDay}" }
                ) { scheduleItem ->
                    DosageScheduleCard(
                        item = scheduleItem,
                        onStatusChange = { newStatus ->
                            // Compute at action time so a session left open past
                            // midnight logs the correct day.
                            val scheduledTimeMillis = todayTimeMillis(
                                scheduleItem.schedule.timeOfDay
                            )
                            scheduleViewModel.recordDoseAction(
                                medicationId = scheduleItem.schedule.medicationId,
                                medicationName = scheduleItem.medicationName,
                                dosage = scheduleItem.dosage,
                                scheduledTime = scheduledTimeMillis,
                                status = newStatus,
                                context = context
                            )
                        }
                    )
                }
            }
        }

        // User Profile Modal Bottom Sheet
        if (showProfileSheet) {
            UserProfileSheet(
                profile = userProfile,
                onSaveProfile = { updated ->
                    profileViewModel.updateProfile(updated)
                    showProfileSheet = false
                },
                onDismiss = { showProfileSheet = false }
            )
        }

        if (showAboutSheet) {
            AboutSheet(onDismiss = { showAboutSheet = false })
        }
    }
}

@Composable
private fun EmptyScheduleState(selectedFilter: ScheduleFilter) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.WbSunny,
                contentDescription = "Empty Schedule",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = when (selectedFilter) {
                    ScheduleFilter.ALL -> "No doses scheduled for today"
                    ScheduleFilter.PENDING -> "No pending doses for today!"
                    ScheduleFilter.TAKEN -> "No doses marked as taken yet"
                    ScheduleFilter.MISSED -> "No missed doses today"
                    ScheduleFilter.SKIPPED -> "No skipped doses today"
                },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Enjoy your peaceful day & stay mindful of your wellness.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
