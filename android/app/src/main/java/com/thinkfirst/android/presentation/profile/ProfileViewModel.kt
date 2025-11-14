package com.thinkfirst.android.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinkfirst.android.data.api.ThinkFirstApi
import com.thinkfirst.android.data.model.BadgeDTO
import com.thinkfirst.android.data.model.LearningProfileDTO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val api: ThinkFirstApi
) : ViewModel() {

    private val _profile = MutableStateFlow<LearningProfileDTO?>(null)
    val profile: StateFlow<LearningProfileDTO?> = _profile.asStateFlow()

    private val _badges = MutableStateFlow<List<BadgeDTO>>(emptyList())
    val badges: StateFlow<List<BadgeDTO>> = _badges.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadProfile(childId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _profile.value = api.getLearningProfile(childId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load profile"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadBadges(childId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _badges.value = api.getBadges(childId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load badges"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markBadgesAsSeen(childId: Long) {
        viewModelScope.launch {
            try {
                api.markBadgesAsSeen(childId)
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }
}

