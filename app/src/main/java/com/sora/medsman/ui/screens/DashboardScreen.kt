package com.sora.medsman.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.NotificationsOff
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.sora.medsman.R
import com.sora.medsman.data.model.TakeStatus
import com.sora.medsman.data.model.UserProfile
import com.sora.medsman.notification.NotificationHelper
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

enum class ScheduleFilter(@StringRes val labelRes: Int) {
    ALL(R.string.dashboard_filter_all),
    PENDING(R.string.dashboard_filter_pending),
    TAKEN(R.string.dashboard_filter_taken),
    MISSED(R.string.dashboard_filter_missed),
    SKIPPED(R.string.dashboard_filter_skipped)
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

    var notificationsEnabled by remember {
        mutableStateOf(NotificationHelper.areDosageNotificationsEnabled(context))
    }
    var exactAlarmsAllowed by remember {
        mutableStateOf(NotificationHelper.canScheduleExactAlarms(context))
    }

    // Permission changes happen in system settings, so re-check on return.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationsEnabled = NotificationHelper.areDosageNotificationsEnabled(context)
        exactAlarmsAllowed = NotificationHelper.canScheduleExactAlarms(context)
    }

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
                                contentDescription = stringResource(R.string.dashboard_user_profile),
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
                    userName = userProfile.name,
                    takenCount = dashboardSummary.todayTakenCount,
                    totalScheduledCount = dashboardSummary.todayScheduledCount,
                    lowStockCount = dashboardSummary.lowStockAlertCount,
                    onProfileClick = { showProfileSheet = true },
                    onLowStockClick = onNavigateToInventory
                )
            }

            // Permission warnings
            if (!notificationsEnabled) {
                item {
                    ReminderWarningCard(
                        icon = Icons.Filled.NotificationsOff,
                        title = stringResource(R.string.reminder_warning_notifications_title),
                        message = stringResource(R.string.reminder_warning_notifications_message),
                        actionLabel = stringResource(R.string.reminder_warning_notifications_action),
                        onAction = { openNotificationSettings(context) }
                    )
                }
            }

            if (!exactAlarmsAllowed) {
                item {
                    ReminderWarningCard(
                        icon = Icons.Filled.Alarm,
                        title = stringResource(R.string.reminder_warning_exact_title),
                        message = stringResource(R.string.reminder_warning_exact_message),
                        actionLabel = stringResource(R.string.reminder_warning_exact_action),
                        onAction = { openExactAlarmSettings(context) }
                    )
                }
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
                            text = stringResource(R.string.dashboard_daily_schedule),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        Text(
                            text = stringResource(
                                R.string.dashboard_scheduled_count,
                                filteredSchedules.size
                            ),
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
                                label = { Text(stringResource(filter.labelRes)) },
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
                                unitsPerDose = scheduleItem.unitsPerDose,
                                scheduledTime = scheduledTimeMillis,
                                status = newStatus,
                                context = context,
                                schedule = scheduleItem.schedule
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
private fun ReminderWarningCard(
    icon: ImageVector,
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                TextButton(
                    onClick = onAction,
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        openAppSettings(context)
    }
}

private fun openExactAlarmSettings(context: Context) {
    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
        data = "package:${context.packageName}".toUri()
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        openAppSettings(context)
    }
}

private fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:${context.packageName}".toUri()
        }
    )
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
                contentDescription = stringResource(R.string.dashboard_empty_icon),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = when (selectedFilter) {
                    ScheduleFilter.ALL -> stringResource(R.string.dashboard_empty_all_title)
                    ScheduleFilter.PENDING -> stringResource(R.string.dashboard_empty_pending_title)
                    ScheduleFilter.TAKEN -> stringResource(R.string.dashboard_empty_taken_title)
                    ScheduleFilter.MISSED -> stringResource(R.string.dashboard_empty_missed_title)
                    ScheduleFilter.SKIPPED -> stringResource(R.string.dashboard_empty_skipped_title)
                },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.dashboard_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
