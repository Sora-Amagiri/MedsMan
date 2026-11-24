package com.sora.medsman.ui.components

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.HighlightOff
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.sora.medsman.R
import com.sora.medsman.data.model.Schedule
import com.sora.medsman.data.model.TakeStatus
import com.sora.medsman.ui.theme.*
import com.sora.medsman.ui.viewmodel.DailyScheduleItem
import com.sora.medsman.util.formatClockTime
import com.sora.medsman.util.formatDoseAmount
import java.util.Calendar

@Composable
fun DosageScheduleCard(
    item: DailyScheduleItem,
    onStatusChange: (TakeStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    var showActionButtonsOverride by remember { mutableStateOf(false) }

    val accentColor = remember(item.colorHex) {
        try {
            Color(item.colorHex.toColorInt())
        } catch (_: Exception) {
            Color(0xFF2196F3)
        }
    }

    val context = LocalContext.current
    val formattedTime = remember(item.schedule.timeOfDay, context) {
        formatTimeOfDay(context, item.schedule.timeOfDay)
    }

    val currentStatus = item.todayStatus
    val isActionTaken = currentStatus != null

    val cardBorderColor by animateColorAsState(
        targetValue = when (currentStatus) {
            TakeStatus.TAKEN -> TakenGreen.copy(alpha = 0.6f)
            TakeStatus.SKIPPED -> SkippedOrange.copy(alpha = 0.6f)
            TakeStatus.MISSED -> MissedRed.copy(alpha = 0.6f)
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        },
        label = "CardBorderColor"
    )

    val cardBgColor by animateColorAsState(
        targetValue = when (currentStatus) {
            TakeStatus.TAKEN -> TakenGreenContainer.copy(alpha = 0.35f)
            TakeStatus.SKIPPED -> SkippedOrangeContainer.copy(alpha = 0.35f)
            TakeStatus.MISSED -> MissedRedContainer.copy(alpha = 0.35f)
            else -> MaterialTheme.colorScheme.surface
        },
        label = "CardBgColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, cardBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Main Info Row: Time, Color Pill, Medication Name, Dosage & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color Accent Indicator Pill & Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accentColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Medication,
                        contentDescription = stringResource(R.string.schedule_medication_icon),
                        tint = accentColor,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Medication Details
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.medicationName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formatDoseAmount(
                                item.unitsPerDose,
                                item.dosage,
                                stringResource(R.string.dose_units_fallback)
                            ),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = stringResource(R.string.schedule_time),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formattedTime,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                // Status Badge or Change Status Button
                if (currentStatus != null && !showActionButtonsOverride) {
                    StatusBadge(
                        status = currentStatus,
                        onChangeClick = { showActionButtonsOverride = true }
                    )
                }
            }

            // Interactive Action Buttons Row (Taken / Missed / Skipped)
            AnimatedVisibility(
                visible = !isActionTaken || showActionButtonsOverride,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        thickness = 1.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.schedule_mark_dose),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // TAKEN Action Button
                        Button(
                            onClick = {
                                showActionButtonsOverride = false
                                onStatusChange(TakeStatus.TAKEN)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TakenGreen,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = stringResource(R.string.status_taken),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.status_taken),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        // MISSED Action Button
                        OutlinedButton(
                            onClick = {
                                showActionButtonsOverride = false
                                onStatusChange(TakeStatus.MISSED)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MissedRed
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, MissedRed),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.HighlightOff,
                                contentDescription = stringResource(R.string.status_missed),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.status_missed),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        // SKIPPED Action Button
                        OutlinedButton(
                            onClick = {
                                showActionButtonsOverride = false
                                onStatusChange(TakeStatus.SKIPPED)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = SkippedOrange
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, SkippedOrange),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = stringResource(R.string.status_skipped),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.status_skipped),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(
    status: TakeStatus,
    onChangeClick: () -> Unit
) {
    val (badgeBg, badgeFg, icon) = when (status) {
        TakeStatus.TAKEN -> Triple(
            TakenGreenContainer,
            TakenGreen,
            Icons.Rounded.CheckCircle
        )
        TakeStatus.SKIPPED -> Triple(
            SkippedOrangeContainer,
            SkippedOrange,
            Icons.Rounded.SkipNext
        )
        TakeStatus.MISSED -> Triple(
            MissedRedContainer,
            MissedRed,
            Icons.Rounded.HighlightOff
        )
        TakeStatus.SNOOZED -> Triple(
            PendingBlueContainer,
            PendingBlue,
            Icons.Filled.MoreTime
        )
    }
    val label = stringResource(statusLabelRes(status))

    Surface(
        onClick = onChangeClick,
        shape = RoundedCornerShape(16.dp),
        color = badgeBg,
        modifier = Modifier.padding(start = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = badgeFg,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = badgeFg
                )
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Redo,
                contentDescription = stringResource(R.string.schedule_edit_status),
                tint = badgeFg.copy(alpha = 0.7f),
                modifier = Modifier
                    .padding(start = 2.dp)
                    .size(12.dp)
            )
        }
    }
}

@StringRes
private fun statusLabelRes(status: TakeStatus): Int = when (status) {
    TakeStatus.TAKEN -> R.string.status_taken
    TakeStatus.MISSED -> R.string.status_missed
    TakeStatus.SKIPPED -> R.string.status_skipped
    TakeStatus.SNOOZED -> R.string.status_snoozed
}

private fun formatTimeOfDay(context: Context, time24: String): String {
    return try {
        val parts = time24.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        formatClockTime(context, calendar.timeInMillis)
    } catch (_: Exception) {
        time24
    }
}

@Preview(showBackground = true)
@Composable
fun DosageScheduleCardPreview() {
    MedsManTheme {
        Column {
            DosageScheduleCard(
                item = DailyScheduleItem(
                    schedule = Schedule(id = 1, medicationId = 1, timeOfDay = "08:00"),
                    medicationName = "Lisinopril",
                    dosage = "10 mg",
                    colorHex = "#2196F3",
                    todayStatus = null
                ),
                onStatusChange = {}
            )

            DosageScheduleCard(
                item = DailyScheduleItem(
                    schedule = Schedule(id = 2, medicationId = 2, timeOfDay = "12:30"),
                    medicationName = "Vitamin D3",
                    dosage = "2000 IU",
                    unitsPerDose = 2,
                    colorHex = "#FF9800",
                    todayStatus = TakeStatus.TAKEN
                ),
                onStatusChange = {}
            )
        }
    }
}
