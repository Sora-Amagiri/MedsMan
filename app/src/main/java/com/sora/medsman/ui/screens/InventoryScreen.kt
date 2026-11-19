package com.sora.medsman.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.sora.medsman.data.model.Inventory
import com.sora.medsman.data.model.Medication
import com.sora.medsman.ui.components.MedicationEditorSheet
import com.sora.medsman.ui.components.NatureBackdrop
import com.sora.medsman.ui.components.NatureScene
import com.sora.medsman.ui.theme.MedsManTheme
import com.sora.medsman.ui.theme.TakenGreen
import com.sora.medsman.ui.theme.TakenGreenContainer
import com.sora.medsman.ui.viewmodel.InventoryItemWithMed
import com.sora.medsman.ui.viewmodel.InventoryUiState
import com.sora.medsman.ui.viewmodel.InventoryViewModel
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var refillTarget by remember { mutableStateOf<InventoryItemWithMed?>(null) }
    var settingsTarget by remember { mutableStateOf<InventoryItemWithMed?>(null) }
    var editorVisible by rememberSaveable { mutableStateOf(false) }
    var editingMedicationId by rememberSaveable { mutableStateOf<Long?>(null) }

    val success = state as? InventoryUiState.Success
    val items = success?.items.orEmpty()
    val lowStockItems = success?.lowStockItems.orEmpty()
    val sortedItems = remember(items) {
        items.sortedWith(
            compareByDescending<InventoryItemWithMed> { it.isLowStock }
                .thenBy { it.medication.name }
        )
    }
    val editingItem = items.find { it.medication.id == editingMedicationId }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pill Inventory",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            editingMedicationId = null
                            editorVisible = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add medication"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        if (state is InventoryUiState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 340.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 4.dp,
                    bottom = 32.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    InventoryHeroCard(
                        totalMedications = items.size,
                        totalUnits = items.sumOf { it.inventory.currentStock },
                        lowStockCount = lowStockItems.size
                    )
                }

                if (lowStockItems.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        LowStockBanner(count = lowStockItems.size)
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your medications",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "${items.size} tracked",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (items.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyInventoryState(
                            onAdd = {
                                editingMedicationId = null
                                editorVisible = true
                            }
                        )
                    }
                } else {
                    items(
                        items = sortedItems,
                        key = { it.medication.id }
                    ) { item ->
                        InventoryCard(
                            item = item,
                            onEdit = {
                                editingMedicationId = item.medication.id
                                editorVisible = true
                            },
                            onRefill = { refillTarget = item },
                            onSettings = { settingsTarget = item },
                            onDecrease = {
                                viewModel.adjustStock(item.medication.id, -1)
                            },
                            onIncrease = {
                                viewModel.adjustStock(item.medication.id, 1)
                            }
                        )
                    }
                }
            }
        }

        refillTarget?.let { target ->
            RefillStockDialog(
                item = target,
                onDismiss = { refillTarget = null },
                onConfirm = { amount ->
                    viewModel.refillStock(target.medication.id, amount, context)
                    refillTarget = null
                }
            )
        }

        settingsTarget?.let { target ->
            InventorySettingsDialog(
                item = target,
                onDismiss = { settingsTarget = null },
                onSave = { updated ->
                    viewModel.updateInventory(updated, context)
                    settingsTarget = null
                }
            )
        }

        if (editorVisible) {
            MedicationEditorSheet(
                existing = editingItem,
                onDismiss = {
                    editorVisible = false
                    editingMedicationId = null
                },
                onSave = { medication, inventory, times ->
                    viewModel.saveMedication(
                        existing = editingItem,
                        medication = medication,
                        inventory = inventory,
                        scheduleTimes = times,
                        context = context
                    )
                    editorVisible = false
                    editingMedicationId = null
                },
                onDelete = if (editingItem != null) {
                    {
                        viewModel.deleteMedication(editingItem, context)
                        editorVisible = false
                        editingMedicationId = null
                    }
                } else {
                    null
                }
            )
        }
    }
}

@Composable
private fun InventoryHeroCard(
    totalMedications: Int,
    totalUnits: Int,
    lowStockCount: Int
) {
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
                scene = NatureScene.SERENE_FOREST,
                modifier = Modifier.matchParentSize()
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0x22000000), Color(0xD9061F18))
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp)
                    .padding(20.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Medication,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Your medicine cabinet",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "$totalUnits unit(s) on hand across $totalMedications medication(s)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeroStatPill(
                        text = "$totalMedications tracked",
                        icon = Icons.Filled.Medication
                    )
                    HeroStatPill(
                        text = if (lowStockCount > 0) {
                            "$lowStockCount low on stock"
                        } else {
                            "All stocked up"
                        },
                        icon = if (lowStockCount > 0) {
                            Icons.Rounded.WarningAmber
                        } else {
                            Icons.Filled.CheckCircle
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroStatPill(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
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
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
            )
        }
    }
}

@Composable
private fun LowStockBanner(count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = "Refill alert",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(26.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Time to refill",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                )
                Text(
                    text = "$count medication(s) at or below the reorder threshold.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun InventoryCard(
    item: InventoryItemWithMed,
    onEdit: () -> Unit,
    onRefill: () -> Unit,
    onSettings: () -> Unit,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    val accentColor = remember(item.medication.colorHex) {
        try {
            Color(item.medication.colorHex.toColorInt())
        } catch (_: Exception) {
            Color(0xFF2D6A4F)
        }
    }

    val stock = item.inventory.currentStock
    val threshold = item.inventory.refillThreshold
    val capacity = max(threshold * 3, max(stock, 1))
    val stockFraction = (stock.toFloat() / capacity.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            width = 1.5.dp,
            color = if (item.isLowStock) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            color = accentColor.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Medication,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.medication.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            append(item.medication.dosage)
                            append(" · ")
                            append(item.medication.form)
                            if (item.medication.unitsPerDose > 1) {
                                append(" · ${item.medication.unitsPerDose} per dose")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                StockBadge(isLowStock = item.isLowStock)

                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit ${item.medication.name}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$stock",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (item.isLowStock) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.inventory.unit,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                    }
                    Text(
                        text = "Refill alert at $threshold ${item.inventory.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StockStepper(
                    medicationName = item.medication.name,
                    canDecrease = stock > 0,
                    onDecrease = onDecrease,
                    onIncrease = onIncrease
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(50)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(stockFraction)
                        .background(
                            color = if (item.isLowStock) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            shape = RoundedCornerShape(50)
                        )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onSettings,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Settings")
                }

                FilledTonalButton(
                    onClick = onRefill,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Refill")
                }
            }
        }
    }
}

@Composable
private fun StockBadge(isLowStock: Boolean) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (isLowStock) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            TakenGreenContainer
        },
        contentColor = if (isLowStock) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            TakenGreen
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isLowStock) {
                    Icons.Rounded.WarningAmber
                } else {
                    Icons.Filled.CheckCircle
                },
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = if (isLowStock) "Low stock" else "Stocked",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun StockStepper(
    medicationName: String,
    canDecrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 2.dp)
        ) {
            IconButton(
                onClick = onDecrease,
                enabled = canDecrease,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Remove,
                    contentDescription = "Decrease stock for $medicationName",
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onIncrease,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Increase stock for $medicationName",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyInventoryState(onAdd: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                imageVector = Icons.Filled.Medication,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No medications tracked yet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Add a medication to start monitoring your supply.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAdd,
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add medication")
            }
        }
    }
}

@Composable
private fun RefillStockDialog(
    item: InventoryItemWithMed,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var amountText by rememberSaveable { mutableStateOf("30") }
    val amount = amountText.toIntOrNull() ?: 0
    val newTotal = item.inventory.currentStock + amount

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
        title = { Text("Refill ${item.medication.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Current stock: ${item.inventory.currentStock} ${item.inventory.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { value ->
                        if (value.length <= 4 && value.all(Char::isDigit)) {
                            amountText = value
                        }
                    },
                    label = { Text("Amount to add") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(10, 30, 60).forEach { quickAmount ->
                        AssistChip(
                            onClick = { amountText = quickAmount.toString() },
                            label = { Text("+$quickAmount") }
                        )
                    }
                }

                Text(
                    text = "New total: $newTotal ${item.inventory.unit}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(amount) },
                enabled = amount > 0
            ) {
                Text("Add stock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun InventorySettingsDialog(
    item: InventoryItemWithMed,
    onDismiss: () -> Unit,
    onSave: (Inventory) -> Unit
) {
    val inventory = item.inventory
    val maxThreshold = max(30, inventory.refillThreshold).toFloat()

    var threshold by remember {
        mutableFloatStateOf(inventory.refillThreshold.coerceIn(1, maxThreshold.toInt()).toFloat())
    }
    var reminderEnabled by remember { mutableStateOf(inventory.refillReminderEnabled) }
    var pharmacyName by remember { mutableStateOf(inventory.pharmacyName) }
    var pharmacyPhone by remember { mutableStateOf(inventory.pharmacyPhone) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Tune, contentDescription = null) },
        title = { Text("Stock settings") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = item.medication.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                Text(
                    text = "Alert me when ${threshold.roundToInt()} ${inventory.unit} remain",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Slider(
                    value = threshold,
                    onValueChange = { threshold = it },
                    valueRange = 1f..maxThreshold,
                    steps = (maxThreshold.toInt() - 2).coerceAtLeast(0)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Refill reminder",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Notify me when stock is low",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { reminderEnabled = it }
                    )
                }

                OutlinedTextField(
                    value = pharmacyName,
                    onValueChange = { pharmacyName = it },
                    label = { Text("Pharmacy (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pharmacyPhone,
                    onValueChange = { pharmacyPhone = it },
                    label = { Text("Pharmacy phone (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        inventory.copy(
                            refillThreshold = threshold.roundToInt(),
                            refillReminderEnabled = reminderEnabled,
                            pharmacyName = pharmacyName.trim(),
                            pharmacyPhone = pharmacyPhone.trim()
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true, widthDp = 420)
@Composable
private fun InventoryComponentsPreview() {
    MedsManTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InventoryHeroCard(totalMedications = 3, totalUnits = 84, lowStockCount = 1)
            InventoryCard(
                item = InventoryItemWithMed(
                    medication = Medication(
                        id = 1,
                        name = "Lisinopril",
                        dosage = "10 mg",
                        unitsPerDose = 2,
                        colorHex = "#2D6A4F"
                    ),
                    inventory = Inventory(
                        medicationId = 1,
                        currentStock = 4,
                        refillThreshold = 5,
                        unit = "pills"
                    ),
                    isLowStock = true
                ),
                onRefill = {},
                onSettings = {},
                onEdit = {},
                onDecrease = {},
                onIncrease = {}
            )
        }
    }
}
