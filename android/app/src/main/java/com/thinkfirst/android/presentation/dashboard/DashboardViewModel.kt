package com.thinkfirst.android.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinkfirst.android.data.api.ThinkFirstApi
import com.thinkfirst.android.data.model.Achievement
import com.thinkfirst.android.data.model.ProgressReport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = false,
    val progress: ProgressReport? = null,
    val achievements: List<Achievement> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val api: ThinkFirstApi
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    fun loadProgress(childId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val progress = api.getProgress(childId)
                val achievements = api.getAchievements(childId)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    progress = progress,
                    achievements = achievements
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load progress"
                )
            }
        }
    }
}

