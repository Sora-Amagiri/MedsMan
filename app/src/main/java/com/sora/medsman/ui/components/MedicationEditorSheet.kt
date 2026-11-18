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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.sora.medsman.data.model.Inventory
import com.sora.medsman.data.model.Medication
import com.sora.medsman.ui.viewmodel.InventoryItemWithMed
import java.time.LocalTime
import java.time.format.DateTimeFormatter
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

    var name by rememberSaveable { mutableStateOf(existingMedication?.name.orEmpty()) }
    var dosage by rememberSaveable { mutableStateOf(existingMedication?.dosage.orEmpty()) }
    var form by rememberSaveable { mutableStateOf(existingMedication?.form ?: "Tablet") }
    var instructions by rememberSaveable { mutableStateOf(existingMedication?.instructions.orEmpty()) }
    var colorHex by rememberSaveable { mutableStateOf(existingMedication?.colorHex ?: "#2D6A4F") }
    var stockText by rememberSaveable {
        mutableStateOf((existingInventory?.currentStock ?: 0).toString())
    }
    var unit by rememberSaveable { mutableStateOf(existingInventory?.unit ?: "pills") }
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
            form = form,
            instructions = instructions.trim(),
            colorHex = colorHex,
            category = existingMedication?.category ?: "Prescription"
        )
    }

    fun buildInventory(): Inventory {
        val base = existingInventory
            ?: Inventory(medicationId = existingMedication?.id ?: 0)
        return base.copy(
            medicationId = existingMedication?.id ?: 0,
            currentStock = stockValue ?: 0,
            unit = unit.trim().ifBlank { "pills" },
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
                        text = if (existing == null) "Add medication" else "Edit medication",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Details, reminders, and stock in one place",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            EditorSectionLabel("Medication")

            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 80) name = it },
                label = { Text("Medication name") },
                placeholder = { Text("e.g. Lisinopril") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = dosage,
                onValueChange = { if (it.length <= 40) dosage = it },
                label = { Text("Dosage") },
                placeholder = { Text("e.g. 10 mg") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Form",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormOptions.forEach { option ->
                    FilterChip(
                        selected = form == option,
                        onClick = { form = option },
                        label = { Text(option) }
                    )
                }
            }

            OutlinedTextField(
                value = instructions,
                onValueChange = { if (it.length <= 200) instructions = it },
                label = { Text("Instructions (optional)") },
                placeholder = { Text("e.g. Take with breakfast") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            EditorSectionLabel("Appearance")

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ColorOptions.forEach { hex ->
                    ColorSwatch(
                        hex = hex,
                        selected = colorHex.equals(hex, ignoreCase = true),
                        onClick = { colorHex = hex }
                    )
                }
            }

            EditorSectionLabel("Reminder times")

            if (reminderTimes.isEmpty()) {
                Text(
                    text = "No reminders set. This medication will be tracked as needed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    reminderTimes.forEach { time ->
                        InputChip(
                            selected = true,
                            onClick = { reminderTimes = reminderTimes - time },
                            label = { Text(displayTime(time, is24HourFormat)) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Remove ${displayTime(time, is24HourFormat)}",
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
                Text("Choose time")
            }

            val quickTimes = QuickTimes.filter { it !in reminderTimes }
            if (quickTimes.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    quickTimes.forEach { time ->
                        AssistChip(
                            onClick = { addReminderTime(time) },
                            label = { Text("+ ${displayTime(time, is24HourFormat)}") }
                        )
                    }
                }
            }

            EditorSectionLabel("Inventory")

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
                    label = { Text("On hand") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { if (it.length <= 12) unit = it },
                    label = { Text("Unit") },
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
                label = { Text("Refill alert threshold") },
                supportingText = {
                    Text(
                        text = "Alert when ${thresholdValue ?: 0} ${unit.ifBlank { "units" }} remain"
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
                        text = "Refill reminder",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Notify me when stock reaches the threshold",
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
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onSave(buildMedication(), buildInventory(), reminderTimes)
                    },
                    enabled = canSave,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(if (existing == null) "Add medication" else "Save changes")
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
                    Text("Delete medication")
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
                    Text("Add")
                }
            },
            title = { Text("Reminder time") },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
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
            title = { Text("Delete ${existing.medication.name}?") },
            text = {
                Text(
                    "This removes the medication, its schedule reminders, and its " +
                        "inventory. Your dosage history will be kept."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
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
                contentDescription = "Selected color",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun displayTime(time24: String, is24Hour: Boolean): String = try {
    LocalTime.parse(time24).format(
        DateTimeFormatter.ofPattern(
            if (is24Hour) "HH:mm" else "h:mm a",
            Locale.getDefault()
        )
    )
} catch (_: Exception) {
    time24
}
