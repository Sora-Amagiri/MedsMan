package com.sora.medsman.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.sora.medsman.R
import com.sora.medsman.data.model.Inventory
import com.sora.medsman.data.model.Medication
import com.sora.medsman.ui.viewmodel.InventoryItemWithMed
import com.sora.medsman.util.formatClockTime
import java.time.LocalTime
import java.util.Calendar
import java.util.Locale

private val FormOptions = listOf("Tablet", "Capsule", "Liquid", "Injection", "Other")
private val ColorOptions = listOf(
    "#2D6A4F",
    "#2196F3",
    "#FF9800",
    "#E91E63",
    "#7E57C2",
    "#00897B"
)
private val QuickTimes = listOf("08:00", "12:00", "20:00")

private fun normalizeForm(form: String?): String =
    FormOptions.find { it.equals(form, ignoreCase = true) } ?: "Tablet"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MedicationEditorSheet(
    existing: InventoryItemWithMed?,
    onDismiss: () -> Unit,
    onSave: (Medication, Inventory, List<String>) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val existingMedication = existing?.medication
    val existingInventory = existing?.inventory
    val defaultUnit = stringResource(R.string.default_unit)
    val existingUnitLabel = existingInventory?.unit?.let { localizedUnit(it) } ?: defaultUnit

    var name by rememberSaveable { mutableStateOf(existingMedication?.name.orEmpty()) }
    var dosage by rememberSaveable { mutableStateOf(existingMedication?.dosage.orEmpty()) }
    var unitsPerDose by rememberSaveable {
        mutableStateOf((existingMedication?.unitsPerDose ?: 1).coerceAtLeast(1))
    }
    var form by rememberSaveable { mutableStateOf(normalizeForm(existingMedication?.form)) }
    var instructions by rememberSaveable { mutableStateOf(existingMedication?.instructions.orEmpty()) }
    var colorHex by rememberSaveable { mutableStateOf(existingMedication?.colorHex ?: "#2D6A4F") }
    var stockText by rememberSaveable {
        mutableStateOf((existingInventory?.currentStock ?: 0).toString())
    }
    var unit by rememberSaveable { mutableStateOf(existingUnitLabel) }
    var thresholdText by rememberSaveable {
        mutableStateOf((existingInventory?.refillThreshold ?: 5).toString())
    }
    var refillReminderEnabled by rememberSaveable {
        mutableStateOf(existingInventory?.refillReminderEnabled ?: true)
    }
    var reminderTimes by rememberSaveable {
        mutableStateOf(
            existing?.schedules
                ?.map { it.timeOfDay }
                ?.distinct()
                ?.sorted()
                ?: listOf("08:00")
        )
    }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val is24HourFormat = remember(context) { DateFormat.is24HourFormat(context) }
    val timePickerState = rememberTimePickerState(
        initialHour = 8,
        initialMinute = 0,
        is24Hour = is24HourFormat
    )

    fun addReminderTime(time: String) {
        val trimmed = time.trim()
        if (trimmed !in reminderTimes) {
            reminderTimes = (reminderTimes + trimmed).sorted()
        }
    }

    val stockValue = stockText.toIntOrNull()
    val thresholdValue = thresholdText.toIntOrNull()
    val canSave = name.isNotBlank() &&
        dosage.isNotBlank() &&
        stockValue != null &&
        thresholdValue != null

    fun buildMedication(): Medication {
        val base = existingMedication ?: Medication(name = "", dosage = "")
        return base.copy(
            name = name.trim(),
            dosage = dosage.trim(),
            unitsPerDose = unitsPerDose,
            form = form,
            instructions = instructions.trim(),
            colorHex = colorHex,
            category = existingMedication?.category ?: "Prescription"
        )
    }

    fun buildInventory(): Inventory {
        val base = existingInventory
            ?: Inventory(medicationId = existingMedication?.id ?: 0)
        val resolvedUnit = unit.trim().ifBlank { defaultUnit }
        unit = if (resolvedUnit == defaultUnit) "pills" else resolvedUnit
        return base.copy(
            medicationId = existingMedication?.id ?: 0,
            currentStock = stockValue ?: 0,
            unit = if (resolvedUnit == defaultUnit) "pills" else resolvedUnit,
            refillThreshold = thresholdValue ?: 0,
            refillReminderEnabled = refillReminderEnabled
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            if (existing == null) R.string.editor_add_title
                            else R.string.editor_edit_title
                        ),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = stringResource(R.string.editor_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.editor_close)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            EditorSectionLabel(stringResource(R.string.editor_section_medication))

            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 80) name = it },
                label = { Text(stringResource(R.string.editor_name)) },
                placeholder = { Text(stringResource(R.string.editor_name_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = dosage,
                onValueChange = { if (it.length <= 40) dosage = it },
                label = { Text(stringResource(R.string.editor_dosage)) },
                placeholder = { Text(stringResource(R.string.editor_dosage_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            UnitsPerDoseRow(
                unitsPerDose = unitsPerDose,
                unitLabel = unit.trim().ifBlank { defaultUnit },
                onUnitsChange = { unitsPerDose = it }
            )

            Text(
                text = stringResource(R.string.editor_form),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormOptions.forEach { option ->
                    FilterChip(
                        selected = form == option,
                        onClick = { form = option },
                        label = { Text(localizedFormLabel(option)) }
                    )
                }
            }

            OutlinedTextField(
                value = instructions,
                onValueChange = { if (it.length <= 200) instructions = it },
                label = { Text(stringResource(R.string.editor_instructions)) },
                placeholder = { Text(stringResource(R.string.editor_instructions_placeholder)) },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            EditorSectionLabel(stringResource(R.string.editor_section_appearance))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ColorOptions.forEach { hex ->
                    ColorSwatch(
                        hex = hex,
                        selected = colorHex.equals(hex, ignoreCase = true),
                        onClick = { colorHex = hex }
                    )
                }
            }

            EditorSectionLabel(stringResource(R.string.editor_section_reminder_times))

            if (reminderTimes.isEmpty()) {
                Text(
                    text = stringResource(R.string.editor_no_reminders),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    reminderTimes.forEach { time ->
                        val displayTime = displayTime(time, context)
                        InputChip(
                            selected = true,
                            onClick = { reminderTimes = reminderTimes - time },
                            label = { Text(displayTime) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(
                                        R.string.editor_remove_time,
                                        displayTime
                                    ),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = { showTimePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.editor_choose_time))
            }

            val quickTimes = QuickTimes.filter { it !in reminderTimes }
            if (quickTimes.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    quickTimes.forEach { time ->
                        AssistChip(
                            onClick = { addReminderTime(time) },
                            label = {
                                Text(
                                    stringResource(
                                        R.string.editor_quick_add_time,
                                        displayTime(time, context)
                                    )
                                )
                            }
                        )
                    }
                }
            }

            EditorSectionLabel(stringResource(R.string.editor_section_inventory))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = stockText,
                    onValueChange = { value ->
                        if (value.length <= 6 && value.all(Char::isDigit)) {
                            stockText = value
                        }
                    },
                    label = { Text(stringResource(R.string.editor_on_hand)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { if (it.length <= 12) unit = it },
                    label = { Text(stringResource(R.string.editor_unit)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = thresholdText,
                onValueChange = { value ->
                    if (value.length <= 6 && value.all(Char::isDigit)) {
                        thresholdText = value
                    }
                },
                label = { Text(stringResource(R.string.editor_refill_threshold)) },
                supportingText = {
                    Text(
                        text = stringResource(
                            R.string.editor_refill_threshold_support,
                            thresholdValue ?: 0,
                            unit.trim().ifBlank { defaultUnit }
                        )
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.editor_refill_reminder),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.editor_refill_reminder_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = refillReminderEnabled,
                    onCheckedChange = { refillReminderEnabled = it }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
                Button(
                    onClick = {
                        onSave(buildMedication(), buildInventory(), reminderTimes)
                    },
                    enabled = canSave,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        stringResource(
                            if (existing == null) R.string.editor_add_title
                            else R.string.editor_save_changes
                        )
                    )
                }
            }

            if (existing != null && onDelete != null) {
                TextButton(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.editor_delete_medication))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showTimePicker) {
        val selectedTime = String.format(
            Locale.US,
            "%02d:%02d",
            timePickerState.hour,
            timePickerState.minute
        )

        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        addReminderTime(selectedTime)
                        showTimePicker = false
                    },
                    enabled = selectedTime !in reminderTimes
                ) {
                    Text(stringResource(R.string.editor_add))
                }
            },
            title = { Text(stringResource(R.string.editor_reminder_time_title)) },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }

    if (showDeleteConfirmation && existing != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(stringResource(R.string.editor_delete_title, existing.medication.name))
            },
            text = {
                Text(stringResource(R.string.editor_delete_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    }
                ) {
                    Text(
                        stringResource(R.string.common_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
private fun UnitsPerDoseRow(
    unitsPerDose: Int,
    unitLabel: String,
    onUnitsChange: (Int) -> Unit
) {
    val fallbackUnit = stringResource(R.string.default_unit)
    val singularUnit = unitLabel.removeSuffix("s").ifBlank { fallbackUnit }
    val unitText = if (unitsPerDose == 1) singularUnit else unitLabel

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.editor_units_per_dose),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(
                    R.string.editor_units_per_dose_description,
                    unitsPerDose,
                    unitText
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onUnitsChange((unitsPerDose - 1).coerceAtLeast(1)) },
                    enabled = unitsPerDose > 1,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Remove,
                        contentDescription = stringResource(R.string.editor_units_decrease),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = unitsPerDose.toString(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(min = 28.dp)
                )
                IconButton(
                    onClick = { onUnitsChange((unitsPerDose + 1).coerceAtMost(99)) },
                    enabled = unitsPerDose < 99,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.editor_units_increase),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun ColorSwatch(
    hex: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val swatchColor = remember(hex) {
        try {
            Color(hex.toColorInt())
        } catch (_: Exception) {
            Color(0xFF2D6A4F)
        }
    }

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(swatchColor)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = stringResource(R.string.editor_selected_color),
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun displayTime(time24: String, context: android.content.Context): String = try {
    val time = LocalTime.parse(time24)
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, time.hour)
        set(Calendar.MINUTE, time.minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    formatClockTime(context, calendar.timeInMillis)
} catch (_: Exception) {
    time24
}
