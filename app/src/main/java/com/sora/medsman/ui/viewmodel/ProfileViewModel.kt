package com.sora.medsman.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sora.medsman.data.model.UserProfile
import com.sora.medsman.data.repository.MedsManRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface ProfileUiState {
    object Loading : ProfileUiState
    data class Success(val userProfile: UserProfile) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

class ProfileViewModel(
    private val repository: MedsManRepository
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = repository.userProfile
        .map { profile ->
            if (profile != null) {
                ProfileUiState.Success(profile)
            } else {
                ProfileUiState.Success(UserProfile())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProfileUiState.Loading
        )

    fun updateProfile(profile: UserProfile) {
        viewModelScope.launch {
            repository.updateUserProfile(profile)
        }
    }

    class Factory(private val repository: MedsManRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfileViewModel(repository) as T
        }
    }
}
