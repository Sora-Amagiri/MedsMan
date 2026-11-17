package com.sora.medsman.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sora.medsman.data.model.Inventory
import com.sora.medsman.data.model.Medication
import com.sora.medsman.data.model.MedicationWithDetails
import com.sora.medsman.data.model.Schedule
import com.sora.medsman.data.repository.MedsManRepository
import com.sora.medsman.notification.NotificationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface MedicationUiState {
    object Loading : MedicationUiState
    data class Success(
        val medications: List<MedicationWithDetails>,
        val categories: List<String>
    ) : MedicationUiState
    data class Error(val message: String) : MedicationUiState
}

class MedicationViewModel(
    private val repository: MedsManRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    val uiState: StateFlow<MedicationUiState> = combine(
        repository.medicationsWithDetails,
        _searchQuery,
        _selectedCategory
    ) { medsWithDetails, query, category ->
        val filtered = medsWithDetails.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.medication.name.contains(query, ignoreCase = true) ||
                    item.medication.instructions.contains(query, ignoreCase = true)
            val matchesCategory = category == null || item.medication.category.equals(category, ignoreCase = true)
            matchesQuery && matchesCategory
        }

        val allCategories = medsWithDetails
            .map { it.medication.category }
            .distinct()
            .sorted()

        MedicationUiState.Success(
            medications = filtered,
            categories = allCategories
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MedicationUiState.Loading
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun addMedication(
        medication: Medication,
        inventory: Inventory?,
        schedules: List<Schedule>,
        context: Context
    ) {
        viewModelScope.launch {
            val medId = repository.insertMedication(medication, inventory, schedules)
            schedules.forEach { schedule ->
                NotificationHelper.scheduleDosageAlarm(
                    context = context,
                    schedule = schedule.copy(medicationId = medId),
                    medicationName = medication.name,
                    dosage = medication.dosage
                )
            }
        }
    }

    fun updateMedication(medication: Medication) {
        viewModelScope.launch {
            repository.updateMedication(medication)
        }
    }

    fun deleteMedication(id: Long, context: Context) {
        viewModelScope.launch {
            val details = repository.getMedicationWithDetails(id).firstOrNull()
            details?.schedules?.forEach { schedule ->
                NotificationHelper.cancelDosageAlarm(context, schedule.id)
            }
            repository.deleteMedication(id)
        }
    }

    class Factory(private val repository: MedsManRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MedicationViewModel(repository) as T
        }
    }
}
