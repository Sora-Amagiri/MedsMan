package com.sora.medsman.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.HighlightOff
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sora.medsman.data.model.HistoryLog
import com.sora.medsman.data.model.TakeStatus
import com.sora.medsman.ui.components.NatureBackdrop
import com.sora.medsman.ui.components.NatureScene
import com.sora.medsman.ui.theme.*
import com.sora.medsman.ui.viewmodel.AdherenceStats
import com.sora.medsman.ui.viewmodel.HistoryUiState
import com.sora.medsman.ui.viewmodel.HistoryViewModel
import com.sora.medsman.util.formatDoseAmount
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val selectedFilter by viewModel.filterStatus.collectAsState()
    var showClearDialog by rememberSaveable { mutableStateOf(false) }

    val success = state as? HistoryUiState.Success
    val logs = remember(success) {
        success?.historyLogs.orEmpty().sortedByDescending { it.scheduledTime }
    }
    val stats = success?.stats ?: AdherenceStats()
    val groups = remember(logs) { groupLogsByDay(logs) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Medication History",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showClearDialog = true },
                        enabled = stats.totalDoses > 0
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = "Clear history"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        if (state is HistoryUiState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val isWide = maxWidth >= 840.dp

                if (isWide) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .width(380.dp)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            HistorySummaryCard(stats = stats)
                            AdherenceLegend(stats = stats)
                        }

                        HistoryLogList(
                            logs = logs,
                            groups = groups,
                            stats = stats,
                            selectedFilter = selectedFilter,
                            onFilterSelected = viewModel::setFilterStatus,
                            onDeleteLog = viewModel::deleteLog,
                            showSummaryCard = false,
                            contentPadding = PaddingValues(bottom = 24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                } else {
                    HistoryLogList(
                        logs = logs,
                        groups = groups,
                        stats = stats,
                        selectedFilter = selectedFilter,
                        onFilterSelected = viewModel::setFilterStatus,
                        onDeleteLog = viewModel::deleteLog,
                        showSummaryCard = true,
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 4.dp,
                            bottom = 24.dp
                        ),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.DeleteSweep,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text("Clear dosage history?") },
                text = {
                    Text(
                        "This permanently removes all dosage records and resets your " +
                            "adherence statistics. This action cannot be undone."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearAllHistory()
                            showClearDialog = false
                        }
                    ) {
                        Text(
                            text = "Clear all",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun HistoryLogList(
    logs: List<HistoryLog>,
    groups: List<Pair<LocalDate, List<HistoryLog>>>,
    stats: AdherenceStats,
    selectedFilter: TakeStatus?,
    onFilterSelected: (TakeStatus?) -> Unit,
    onDeleteLog: (Long) -> Unit,
    showSummaryCard: Boolean,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showSummaryCard) {
            item(key = "summary") {
                HistorySummaryCard(stats = stats)
            }
        }

        item(key = "filters") {
            HistoryFilterRow(
                selectedFilter = selectedFilter,
                onFilterSelected = onFilterSelected,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (logs.isEmpty()) {
            item(key = "empty") {
                EmptyHistoryState(
                    hasAnyHistory = stats.totalDoses > 0,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        } else {
            groups.forEach { (date, dayLogs) ->
                item(key = "header_$date") {
                    DayHeader(
                        label = dayLabel(date),
                        count = dayLogs.size,
                        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
                    )
                }

                items(
                    items = dayLogs,
                    key = { log -> log.id }
                ) { log ->
                    HistoryLogCard(
                        log = log,
                        onDelete = { onDeleteLog(log.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistorySummaryCard(stats: AdherenceStats) {
    val animatedProgress by animateFloatAsState(
        targetValue = stats.adherencePercentage / 100f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "HistoryAdherenceProgress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            NatureBackdrop(
                scene = NatureScene.MISTY_MOUNTAINS,
                modifier = Modifier.matchParentSize()
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0x33000000), Color(0xD90A2420))
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 250.dp)
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Insights,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Adherence overview",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(88.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.25f),
                            strokeWidth = 8.dp
                        )
                        Text(
                            text = "${stats.adherencePercentage}%",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${stats.takenCount} of ${stats.totalDoses} doses taken",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = if (stats.totalDoses == 0) {
                                "Log a dose to start tracking your rhythm"
                            } else {
                                "Every mindful dose counts toward your wellness"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryPill(
                        label = "Taken",
                        count = stats.takenCount,
                        icon = Icons.Rounded.CheckCircle
                    )
                    SummaryPill(
                        label = "Missed",
                        count = stats.missedCount,
                        icon = Icons.Rounded.HighlightOff
                    )
                    SummaryPill(
                        label = "Skipped",
                        count = stats.skippedCount,
                        icon = Icons.Rounded.SkipNext
                    )
                    if (stats.snoozedCount > 0) {
                        SummaryPill(
                            label = "Snoozed",
                            count = stats.snoozedCount,
                            icon = Icons.Filled.MoreTime
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryPill(
    label: String,
    count: Int,
    icon: ImageVector
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.20f),
        contentColor = Color.White
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "$count $label",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
            )
        }
    }
}

@Composable
private fun AdherenceLegend(stats: AdherenceStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Status breakdown",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )

            LegendRow(
                label = "Taken",
                count = stats.takenCount,
                total = stats.totalDoses,
                color = TakenGreen
            )
            LegendRow(
                label = "Missed",
                count = stats.missedCount,
                total = stats.totalDoses,
                color = MissedRed
            )
            LegendRow(
                label = "Skipped",
                count = stats.skippedCount,
                total = stats.totalDoses,
                color = SkippedOrange
            )
            if (stats.snoozedCount > 0) {
                LegendRow(
                    label = "Snoozed",
                    count = stats.snoozedCount,
                    total = stats.totalDoses,
                    color = PendingBlue
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )

            Text(
                text = "Adherence is the share of logged doses you marked as taken.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LegendRow(
    label: String,
    count: Int,
    total: Int,
    color: Color
) {
    val percentage = if (total > 0) (count * 100) / total else 0

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color = color, shape = RoundedCornerShape(50))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$count · $percentage%",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HistoryFilterRow(
    selectedFilter: TakeStatus?,
    onFilterSelected: (TakeStatus?) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        null to "All",
        TakeStatus.TAKEN to "Taken",
        TakeStatus.MISSED to "Missed",
        TakeStatus.SKIPPED to "Skipped",
        TakeStatus.SNOOZED to "Snoozed"
    )

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(options) { (status, label) ->
            FilterChip(
                selected = selectedFilter == status,
                onClick = { onFilterSelected(status) },
                label = { Text(label) },
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}
@Composable
private fun DayHeader(
    label: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "$count dose(s)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HistoryLogCard(
    log: HistoryLog,
    onDelete: () -> Unit
) {
    val visual = statusVisual(log.status)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = visual.container,
                        shape = RoundedCornerShape(13.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = visual.icon,
                    contentDescription = visual.label,
                    tint = visual.color,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = log.medicationName,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    HistoryStatusChip(visual = visual)
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${formatDoseAmount(log.unitsPerDose, log.dosage)} · " +
                        "Scheduled ${formatTime(log.scheduledTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Logged ${formatDateTime(log.actionTime)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                if (!log.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = log.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete ${log.medicationName} log",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun HistoryStatusChip(visual: StatusVisual) {
    Surface(
        shape = RoundedCornerShape(50),
        color = visual.container,
        contentColor = visual.color
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = visual.icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = visual.label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun EmptyHistoryState(
    hasAnyHistory: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (hasAnyHistory) Icons.Filled.History else Icons.Filled.EventNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (hasAnyHistory) {
                    "No doses match this filter"
                } else {
                    "No dosage history yet"
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (hasAnyHistory) {
                    "Try selecting a different status to see more records."
                } else {
                    "Doses you mark from the Today screen will appear here."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private data class StatusVisual(
    val color: Color,
    val container: Color,
    val icon: ImageVector,
    val label: String
)

private fun statusVisual(status: TakeStatus): StatusVisual = when (status) {
    TakeStatus.TAKEN -> StatusVisual(
        color = TakenGreen,
        container = TakenGreenContainer,
        icon = Icons.Rounded.CheckCircle,
        label = "Taken"
    )

    TakeStatus.MISSED -> StatusVisual(
        color = MissedRed,
        container = MissedRedContainer,
        icon = Icons.Rounded.HighlightOff,
        label = "Missed"
    )

    TakeStatus.SKIPPED -> StatusVisual(
        color = SkippedOrange,
        container = SkippedOrangeContainer,
        icon = Icons.Rounded.SkipNext,
        label = "Skipped"
    )

    TakeStatus.SNOOZED -> StatusVisual(
        color = PendingBlue,
        container = PendingBlueContainer,
        icon = Icons.Filled.MoreTime,
        label = "Snoozed"
    )
}

private fun formatTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
        .format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))

private fun formatDateTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.getDefault()))

private fun dayLabel(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(
            DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())
        )
    }
}

private fun groupLogsByDay(
    logs: List<HistoryLog>
): List<Pair<LocalDate, List<HistoryLog>>> =
    logs.sortedByDescending { it.scheduledTime }
        .groupBy { log ->
            Instant.ofEpochMilli(log.scheduledTime)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }
        .toList()

@Preview(showBackground = true, widthDp = 420)
@Composable
private fun HistoryComponentsPreview() {
    val now = System.currentTimeMillis()

    MedsManTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HistorySummaryCard(
                stats = AdherenceStats(
                    totalDoses = 12,
                    takenCount = 9,
                    skippedCount = 1,
                    missedCount = 2,
                    adherencePercentage = 75
                )
            )

            HistoryLogCard(
                log = HistoryLog(
                    id = 1,
                    medicationId = 1,
                    medicationName = "Lisinopril",
                    dosage = "10 mg",
                    unitsPerDose = 2,
                    scheduledTime = now,
                    actionTime = now,
                    status = TakeStatus.TAKEN
                ),
                onDelete = {}
            )
        }
    }
}
